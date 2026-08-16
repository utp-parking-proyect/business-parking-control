package com.utp.control.service.impl;

import com.utp.control.client.request.ParkingRequestServiceClient;
import com.utp.control.generated.model.AccessControlIn;
import com.utp.control.generated.model.AccessControlOut;
import com.utp.control.generated.model.AccessControlResult;
import com.utp.control.generated.model.LocationAvailability;
import com.utp.control.generated.model.ParkingAuthorization;
import com.utp.control.generated.model.ParkingAuthorizationResult;
import com.utp.control.mapper.AccessControlMapper;
import com.utp.control.model.entity.Location;
import com.utp.control.model.entity.Record;
import com.utp.control.repository.LocationRepository;
import com.utp.control.repository.RecordRepository;
import com.utp.control.service.AccessControlService;
import com.utp.control.service.AvailabilityEventPublisher;
import com.utp.control.service.AvailabilityService;
import com.utp.control.util.Constants;
import com.utp.control.util.NumberPlateNormalizer;
import com.utp.control.util.error.ConflictException;
import com.utp.control.util.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccessControlServiceImpl implements AccessControlService {

  private final LocationRepository locationRepository;
  private final RecordRepository recordRepository;
  private final ParkingRequestServiceClient parkingRequestServiceClient;
  private final AvailabilityService availabilityService;
  private final AvailabilityEventPublisher availabilityEventPublisher;
  private final AccessControlMapper accessControlMapper;
  private final TransactionalOperator transactionalOperator;

  private record MovementOutcome(AccessControlResult result, Integer idRecord,
                                 LocalDateTime entryDate, LocalDateTime departureDate) {

    static MovementOutcome rejected(AccessControlResult result) {
      return new MovementOutcome(result, null, null, null);
    }
  }

  @Override
  public Mono<AccessControlOut> registerEntry(Long securityUserId, AccessControlIn movement) {
    String numberPlate = NumberPlateNormalizer.normalize(movement.getNumberPlate());
    Integer locationId = requireLocation(movement.getLocationId());

    log.info("Intento de ingreso - NumberPlate: {}, LocationId: {}", numberPlate, locationId);

    return findLocation(locationId)
        .flatMap(location -> parkingRequestServiceClient.getParkingAuthorization(numberPlate)
            .flatMap(authorization -> entryOutcome(securityUserId, location, authorization,
                movement.getObservation())
                .flatMap(outcome -> buildResponse(location, authorization, outcome))));
  }

  @Override
  public Mono<AccessControlOut> registerExit(Long securityUserId, AccessControlIn movement) {
    String numberPlate = NumberPlateNormalizer.normalize(movement.getNumberPlate());
    Integer locationId = requireLocation(movement.getLocationId());

    log.info("Intento de salida - NumberPlate: {}, LocationId: {}", numberPlate, locationId);

    return findLocation(locationId)
        .flatMap(location -> parkingRequestServiceClient.getParkingAuthorization(numberPlate)
            .flatMap(authorization -> exitOutcome(location, authorization,
                movement.getObservation())
                .flatMap(outcome -> buildResponse(location, authorization, outcome))));
  }

  private Mono<MovementOutcome> entryOutcome(Long securityUserId, Location location,
                                             ParkingAuthorization authorization,
                                             String observation) {
    AccessControlResult rejection = rejectionOf(authorization);
    if (rejection != null) {
      log.info("Ingreso rechazado - NumberPlate: {}, LocationId: {}, motivo: {}",
          authorization.getNumberPlate(), location.getIdLocation(), rejection);
      return Mono.just(MovementOutcome.rejected(rejection));
    }

    return createEntryRecord(securityUserId, location, authorization, observation);
  }

  private Mono<MovementOutcome> createEntryRecord(Long securityUserId, Location location,
                                                  ParkingAuthorization authorization,
                                                  String observation) {
    Integer idVehicle = authorization.getIdVehicle();
    LocalDateTime entryDate = LocalDateTime.now();
    int capacity = location.getParkingCapacity() == null ? 0 : location.getParkingCapacity();

    return locationRepository.findByIdForUpdate(location.getIdLocation())
        .then(recordRepository.findByIdVehicleAndDepartureDateIsNull(idVehicle).hasElement())
        .flatMap(alreadyInside -> Boolean.TRUE.equals(alreadyInside)
            ? Mono.just(MovementOutcome.rejected(AccessControlResult.VEHICLE_ALREADY_INSIDE))
            : insertIfAvailable(securityUserId, location, idVehicle, entryDate, observation,
                capacity))
        .as(transactionalOperator::transactional)
        .onErrorMap(DuplicateKeyException.class,
            error -> new ConflictException(Constants.ERROR_ENTRY_ALREADY_REGISTERED));
  }

  private Mono<MovementOutcome> insertIfAvailable(Long securityUserId, Location location,
                                                  Integer idVehicle, LocalDateTime entryDate,
                                                  String observation, int capacity) {
    return recordRepository.countByIdLocationAndDepartureDateIsNull(location.getIdLocation())
        .defaultIfEmpty(0L)
        .flatMap(occupied -> occupied >= capacity
            ? Mono.just(MovementOutcome.rejected(AccessControlResult.PARKING_FULL))
            : recordRepository.insertRecord(idVehicle, location.getIdLocation(),
                    securityUserId.intValue(), entryDate, observation)
                .map(idRecord -> new MovementOutcome(AccessControlResult.ENTRY_REGISTERED,
                    idRecord, entryDate, null)));
  }

  private Mono<MovementOutcome> exitOutcome(Location location, ParkingAuthorization authorization,
                                            String observation) {
    if (authorization.getIdVehicle() == null) {
      log.info("Salida rechazada - NumberPlate: {}, motivo: {}",
          authorization.getNumberPlate(), AccessControlResult.VEHICLE_NOT_FOUND);
      return Mono.just(MovementOutcome.rejected(AccessControlResult.VEHICLE_NOT_FOUND));
    }

    LocalDateTime departureDate = LocalDateTime.now();

    return recordRepository.findByIdVehicleAndDepartureDateIsNull(authorization.getIdVehicle())
        .flatMap(openRecord -> closeRecord(location, openRecord, departureDate, observation))
        .switchIfEmpty(Mono.fromSupplier(
            () -> MovementOutcome.rejected(AccessControlResult.NO_OPEN_RECORD)))
        .as(transactionalOperator::transactional);
  }

  private Mono<MovementOutcome> closeRecord(Location location, Record openRecord,
                                            LocalDateTime departureDate, String observation) {
    if (!location.getIdLocation().equals(openRecord.getIdLocation())) {
      return Mono.just(MovementOutcome.rejected(AccessControlResult.RECORD_AT_OTHER_LOCATION));
    }

    return recordRepository.closeRecord(openRecord.getIdRecord(), departureDate, observation)
        .defaultIfEmpty(0L)
        .flatMap(closedRecords -> closedRecords == 0
            ? Mono.error(new ConflictException(Constants.ERROR_EXIT_ALREADY_REGISTERED))
            : Mono.just(new MovementOutcome(AccessControlResult.EXIT_REGISTERED,
                openRecord.getIdRecord(), openRecord.getEntryDate(), departureDate)));
  }

  private Mono<AccessControlOut> buildResponse(Location location,
                                               ParkingAuthorization authorization,
                                               MovementOutcome outcome) {
    boolean registered = isRegistered(outcome.result());

    return availabilityService.getLocationAvailability(location.getIdLocation())
        .flatMap(availability -> publishIfRegistered(registered, availability))
        .map(availability -> toAccessControlOut(location, authorization, outcome, availability,
            registered));
  }

  private Mono<LocationAvailability> publishIfRegistered(boolean registered,
                                                         LocationAvailability availability) {
    if (!registered) {
      return Mono.just(availability);
    }
    return availabilityEventPublisher.publishAvailabilityChanged(availability)
        .thenReturn(availability);
  }

  private AccessControlOut toAccessControlOut(Location location,
                                              ParkingAuthorization authorization,
                                              MovementOutcome outcome,
                                              LocationAvailability availability,
                                              boolean registered) {
    if (registered) {
      log.info("Movimiento registrado - NumberPlate: {}, LocationId: {}, resultado: {}, "
              + "disponibles: {}", authorization.getNumberPlate(), location.getIdLocation(),
          outcome.result(), availability.getAvailable());
    }

    return new AccessControlOut()
        .authorized(registered)
        .result(outcome.result())
        .message(messageOf(outcome.result()))
        .vehicle(authorization.getVehicle())
        .applicant(authorization.getApplicant())
        .location(accessControlMapper.toLocationInformation(location, availability.getCampusName()))
        .availability(availability)
        .record(accessControlMapper.toAccessRecord(outcome.idRecord(), outcome.entryDate(),
            outcome.departureDate()));
  }

  private Mono<Location> findLocation(Integer locationId) {
    return locationRepository.findById(locationId)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_LOCATION_NOT_FOUND)));
  }

  private Integer requireLocation(Integer locationId) {
    if (locationId == null) {
      throw new IllegalArgumentException(Constants.ERROR_LOCATION_REQUIRED);
    }
    return locationId;
  }

  private boolean isRegistered(AccessControlResult result) {
    return AccessControlResult.ENTRY_REGISTERED.equals(result)
        || AccessControlResult.EXIT_REGISTERED.equals(result);
  }

  private AccessControlResult rejectionOf(ParkingAuthorization authorization) {
    if (Boolean.TRUE.equals(authorization.getAuthorized())) {
      return null;
    }

    ParkingAuthorizationResult result = authorization.getResult();
    if (ParkingAuthorizationResult.VEHICLE_NOT_FOUND.equals(result)) {
      return AccessControlResult.VEHICLE_NOT_FOUND;
    }
    return ParkingAuthorizationResult.VEHICLE_UNASSIGNED.equals(result)
        ? AccessControlResult.VEHICLE_UNASSIGNED
        : AccessControlResult.REQUEST_NOT_APPROVED;
  }

  private String messageOf(AccessControlResult result) {
    return switch (result) {
      case ENTRY_REGISTERED -> Constants.MESSAGE_ENTRY_REGISTERED;
      case EXIT_REGISTERED -> Constants.MESSAGE_EXIT_REGISTERED;
      case VEHICLE_NOT_FOUND -> Constants.MESSAGE_VEHICLE_NOT_FOUND;
      case VEHICLE_UNASSIGNED -> Constants.MESSAGE_VEHICLE_UNASSIGNED;
      case REQUEST_NOT_APPROVED -> Constants.MESSAGE_REQUEST_NOT_APPROVED;
      case VEHICLE_ALREADY_INSIDE -> Constants.MESSAGE_VEHICLE_ALREADY_INSIDE;
      case PARKING_FULL -> Constants.MESSAGE_PARKING_FULL;
      case NO_OPEN_RECORD -> Constants.MESSAGE_NO_OPEN_RECORD;
      case RECORD_AT_OTHER_LOCATION -> Constants.MESSAGE_RECORD_AT_OTHER_LOCATION;
    };
  }
}
