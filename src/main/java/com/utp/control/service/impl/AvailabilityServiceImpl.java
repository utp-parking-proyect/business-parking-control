package com.utp.control.service.impl;

import com.utp.control.client.portal.PortalServiceClient;
import com.utp.control.generated.client.users.model.CampusResponse;
import com.utp.control.generated.model.LocationAvailability;
import com.utp.control.generated.model.ParkingAvailabilityList;
import com.utp.control.model.entity.Location;
import com.utp.control.repository.LocationRepository;
import com.utp.control.repository.RecordRepository;
import com.utp.control.service.AvailabilityService;
import com.utp.control.util.Constants;
import com.utp.control.util.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

  private final LocationRepository locationRepository;
  private final RecordRepository recordRepository;
  private final PortalServiceClient portalServiceClient;

  @Override
  public Mono<ParkingAvailabilityList> getAvailability() {
    return getAvailabilityStream()
        .collectList()
        .map(availability -> new ParkingAvailabilityList().availability(availability));
  }

  @Override
  public Flux<LocationAvailability> getAvailabilityStream() {
    return campusNames().flatMapMany(campusNames -> locationRepository.findAllByOrderByIdLocation()
        .concatMap(location -> toAvailability(location, campusNames.get(location.getIdCampus()))));
  }

  @Override
  public Mono<LocationAvailability> getLocationAvailability(Integer locationId) {
    return locationRepository.findById(locationId)
        .switchIfEmpty(Mono.error(new NotFoundException(Constants.ERROR_LOCATION_NOT_FOUND)))
        .flatMap(location -> campusName(location.getIdCampus())
            .flatMap(name -> toAvailability(location, name))
            .switchIfEmpty(Mono.defer(() -> toAvailability(location, null))));
  }

  private Mono<LocationAvailability> toAvailability(Location location, String campusName) {
    return recordRepository.countByIdLocationAndDepartureDateIsNull(location.getIdLocation())
        .defaultIfEmpty(0L)
        .map(occupied -> buildAvailability(location, campusName, occupied));
  }

  private LocationAvailability buildAvailability(Location location, String campusName,
                                                 long occupied) {
    int capacity = location.getParkingCapacity() == null ? 0 : location.getParkingCapacity();

    if (occupied > capacity) {
      log.warn("Ocupación inconsistente en la sede {}: {} vehículos dentro con capacidad {}",
          location.getIdLocation(), occupied, capacity);
    }

    int available = Math.max(0, capacity - (int) occupied);

    return new LocationAvailability()
        .locationId(location.getIdLocation())
        .locationName(location.getNameLocation())
        .campusId(location.getIdCampus())
        .campusName(campusName)
        .capacity(capacity)
        .occupied((int) occupied)
        .available(available)
        .updatedAt(LocalDateTime.now().toString());
  }

  private Mono<Map<Integer, String>> campusNames() {
    return portalServiceClient.getAllCampus()
        .filter(campus -> campus.getIdCampus() != null)
        .collectMap(campus -> campus.getIdCampus().intValue(), CampusResponse::getNameCampus)
        .onErrorResume(error -> {
          log.error("No se pudo obtener el nombre de los campus desde business-core-portal: {}",
              error.getMessage());
          return Mono.just(Map.<Integer, String>of());
        });
  }

  private Mono<String> campusName(Integer idCampus) {
    if (idCampus == null) {
      return Mono.empty();
    }
    return portalServiceClient.getCampusById(idCampus.longValue())
        .mapNotNull(CampusResponse::getNameCampus)
        .onErrorResume(error -> {
          log.error("No se pudo obtener el campus {} desde business-core-portal: {}",
              idCampus, error.getMessage());
          return Mono.empty();
        });
  }
}
