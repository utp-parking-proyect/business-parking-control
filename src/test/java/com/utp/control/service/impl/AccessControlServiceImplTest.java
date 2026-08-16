package com.utp.control.service.impl;

import com.utp.control.client.request.ParkingRequestServiceClient;
import com.utp.control.generated.model.AccessControlIn;
import com.utp.control.generated.model.AccessControlResult;
import com.utp.control.generated.model.LocationAvailability;
import com.utp.control.generated.model.ParkingAuthorization;
import com.utp.control.generated.model.ParkingAuthorizationResult;
import com.utp.control.generated.model.VehicleInformation;
import com.utp.control.mapper.AccessControlMapperImpl;
import com.utp.control.model.entity.Location;
import com.utp.control.model.entity.Record;
import com.utp.control.repository.LocationRepository;
import com.utp.control.repository.RecordRepository;
import com.utp.control.service.AvailabilityEventPublisher;
import com.utp.control.service.AvailabilityService;
import com.utp.control.util.Constants;
import com.utp.control.util.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceImplTest {

  private static final Long SECURITY_USER_ID = 10L;
  private static final Integer LOCATION_ID = 1;
  private static final Integer VEHICLE_ID = 25;
  private static final String NUMBER_PLATE = "ABC-123";

  @Mock
  private LocationRepository locationRepository;
  @Mock
  private RecordRepository recordRepository;
  @Mock
  private ParkingRequestServiceClient parkingRequestServiceClient;
  @Mock
  private AvailabilityService availabilityService;
  @Mock
  private AvailabilityEventPublisher availabilityEventPublisher;
  @Mock
  private TransactionalOperator transactionalOperator;

  @InjectMocks
  private AccessControlServiceImpl accessControlService;

  @BeforeEach
  void mockTransactionalOperator() {
    Mockito.lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0, Mono.class));
  }

  @BeforeEach
  void useRealMapper() {
    ReflectionTestUtils.setField(accessControlService, "accessControlMapper",
        new AccessControlMapperImpl());
  }

  private Location location(int capacity) {
    Location location = new Location();
    location.setIdLocation(LOCATION_ID);
    location.setIdCampus(1);
    location.setNameLocation("Sede Arequipa");
    location.setAddress("Av. Arequipa 265 - 279");
    location.setParkingCapacity(capacity);
    return location;
  }

  private ParkingAuthorization authorized() {
    return new ParkingAuthorization()
        .authorized(true)
        .result(ParkingAuthorizationResult.AUTHORIZED)
        .idVehicle(VEHICLE_ID)
        .numberPlate(NUMBER_PLATE)
        .idRequest(7)
        .vehicle(new VehicleInformation().numberPlate(NUMBER_PLATE).vehicleType("Automóvil"));
  }

  private ParkingAuthorization rejected(ParkingAuthorizationResult result, Integer idVehicle) {
    return new ParkingAuthorization()
        .authorized(false)
        .result(result)
        .idVehicle(idVehicle)
        .numberPlate(NUMBER_PLATE);
  }

  private LocationAvailability availability(int occupied, int available) {
    return new LocationAvailability()
        .locationId(LOCATION_ID)
        .locationName("Sede Arequipa")
        .campusId(1)
        .campusName("Lima Centro")
        .capacity(75)
        .occupied(occupied)
        .available(available)
        .updatedAt(LocalDateTime.now().toString());
  }

  private Record openRecord(Integer idLocation) {
    Record record = new Record();
    record.setIdRecord(100);
    record.setIdVehicle(VEHICLE_ID);
    record.setIdLocation(idLocation);
    record.setIdSecurity(SECURITY_USER_ID.intValue());
    record.setEntryDate(LocalDateTime.now().minusHours(2));
    return record;
  }

  private AccessControlIn movement() {
    return new AccessControlIn().numberPlate(NUMBER_PLATE).locationId(LOCATION_ID);
  }

  private void mockLocationFound(int capacity) {
    when(locationRepository.findById(LOCATION_ID)).thenReturn(Mono.just(location(capacity)));
  }

  private void mockAvailability(LocationAvailability availability) {
    when(availabilityService.getLocationAvailability(LOCATION_ID))
        .thenReturn(Mono.just(availability));
  }

  @Test
  void testRegisterEntry_AuthorizedVehicleCreatesRecordAndPublishesAvailability() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(authorized()));
    when(locationRepository.findByIdForUpdate(LOCATION_ID)).thenReturn(Mono.just(location(75)));
    when(recordRepository.findByIdVehicleAndDepartureDateIsNull(VEHICLE_ID))
        .thenReturn(Mono.empty());
    when(recordRepository.countByIdLocationAndDepartureDateIsNull(LOCATION_ID))
        .thenReturn(Mono.just(42L));
    when(recordRepository.insertRecord(eq(VEHICLE_ID), eq(LOCATION_ID),
        eq(SECURITY_USER_ID.intValue()), any(), any())).thenReturn(Mono.just(500));
    mockAvailability(availability(43, 32));
    when(availabilityEventPublisher.publishAvailabilityChanged(any())).thenReturn(Mono.empty());

    StepVerifier.create(accessControlService.registerEntry(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertTrue(result.getAuthorized());
          assertEquals(AccessControlResult.ENTRY_REGISTERED, result.getResult());
          assertEquals(Constants.MESSAGE_ENTRY_REGISTERED, result.getMessage());
          assertEquals(500, result.getRecord().getIdRecord());
          assertNull(result.getRecord().getDepartureDate());
          assertEquals("Sede Arequipa", result.getLocation().getNameLocation());
          assertEquals("Lima Centro", result.getLocation().getCampusName());
          assertEquals(32, result.getAvailability().getAvailable());
          assertNotNull(result.getVehicle());
        })
        .verifyComplete();

    verify(availabilityEventPublisher, times(1)).publishAvailabilityChanged(any());
  }

  @Test
  void testRegisterEntry_UnknownVehicleIsRejected() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(rejected(ParkingAuthorizationResult.VEHICLE_NOT_FOUND, null)));
    mockAvailability(availability(43, 32));

    StepVerifier.create(accessControlService.registerEntry(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertFalse(result.getAuthorized());
          assertEquals(AccessControlResult.VEHICLE_NOT_FOUND, result.getResult());
          assertEquals(Constants.MESSAGE_VEHICLE_NOT_FOUND, result.getMessage());
          assertNull(result.getRecord());
        })
        .verifyComplete();

    verify(recordRepository, never()).insertRecord(any(), any(), any(), any(), any());
    verify(availabilityEventPublisher, never()).publishAvailabilityChanged(any());
  }

  @Test
  void testRegisterEntry_UnassignedVehicleIsRejected() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(
            rejected(ParkingAuthorizationResult.VEHICLE_UNASSIGNED, VEHICLE_ID)));
    mockAvailability(availability(43, 32));

    StepVerifier.create(accessControlService.registerEntry(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertFalse(result.getAuthorized());
          assertEquals(AccessControlResult.VEHICLE_UNASSIGNED, result.getResult());
        })
        .verifyComplete();

    verify(recordRepository, never()).insertRecord(any(), any(), any(), any(), any());
  }

  @Test
  void testRegisterEntry_VehicleWithoutApprovedRequestIsRejected() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(
            rejected(ParkingAuthorizationResult.REQUEST_NOT_APPROVED, VEHICLE_ID)));
    mockAvailability(availability(43, 32));

    StepVerifier.create(accessControlService.registerEntry(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertFalse(result.getAuthorized());
          assertEquals(AccessControlResult.REQUEST_NOT_APPROVED, result.getResult());
          assertEquals(Constants.MESSAGE_REQUEST_NOT_APPROVED, result.getMessage());
        })
        .verifyComplete();

    verify(recordRepository, never()).insertRecord(any(), any(), any(), any(), any());
  }

  @Test
  void testRegisterEntry_VehicleAlreadyInsideIsRejected() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(authorized()));
    when(locationRepository.findByIdForUpdate(LOCATION_ID)).thenReturn(Mono.just(location(75)));
    when(recordRepository.findByIdVehicleAndDepartureDateIsNull(VEHICLE_ID))
        .thenReturn(Mono.just(openRecord(LOCATION_ID)));
    mockAvailability(availability(43, 32));

    StepVerifier.create(accessControlService.registerEntry(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertFalse(result.getAuthorized());
          assertEquals(AccessControlResult.VEHICLE_ALREADY_INSIDE, result.getResult());
        })
        .verifyComplete();

    verify(recordRepository, never()).insertRecord(any(), any(), any(), any(), any());
    verify(availabilityEventPublisher, never()).publishAvailabilityChanged(any());
  }

  @Test
  void testRegisterEntry_FullParkingIsRejected() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(authorized()));
    when(locationRepository.findByIdForUpdate(LOCATION_ID)).thenReturn(Mono.just(location(75)));
    when(recordRepository.findByIdVehicleAndDepartureDateIsNull(VEHICLE_ID))
        .thenReturn(Mono.empty());
    when(recordRepository.countByIdLocationAndDepartureDateIsNull(LOCATION_ID))
        .thenReturn(Mono.just(75L));
    mockAvailability(availability(75, 0));

    StepVerifier.create(accessControlService.registerEntry(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertFalse(result.getAuthorized());
          assertEquals(AccessControlResult.PARKING_FULL, result.getResult());
          assertEquals(Constants.MESSAGE_PARKING_FULL, result.getMessage());
          assertEquals(0, result.getAvailability().getAvailable());
        })
        .verifyComplete();

    verify(recordRepository, never()).insertRecord(any(), any(), any(), any(), any());
    verify(availabilityEventPublisher, never()).publishAvailabilityChanged(any());
  }

  @Test
  void testRegisterEntry_LocksLocationBeforeCountingOpenRecords() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(authorized()));
    when(locationRepository.findByIdForUpdate(LOCATION_ID)).thenReturn(Mono.just(location(75)));
    when(recordRepository.findByIdVehicleAndDepartureDateIsNull(VEHICLE_ID))
        .thenReturn(Mono.empty());
    when(recordRepository.countByIdLocationAndDepartureDateIsNull(LOCATION_ID))
        .thenReturn(Mono.just(1L));
    when(recordRepository.insertRecord(any(), any(), any(), any(), any()))
        .thenReturn(Mono.just(501));
    mockAvailability(availability(2, 73));
    when(availabilityEventPublisher.publishAvailabilityChanged(any())).thenReturn(Mono.empty());

    StepVerifier.create(accessControlService.registerEntry(SECURITY_USER_ID, movement()))
        .expectNextCount(1)
        .verifyComplete();

    InOrder inOrder = Mockito.inOrder(locationRepository, recordRepository);
    inOrder.verify(locationRepository).findByIdForUpdate(LOCATION_ID);
    inOrder.verify(recordRepository).countByIdLocationAndDepartureDateIsNull(LOCATION_ID);
    inOrder.verify(recordRepository).insertRecord(any(), any(), any(), any(), any());
  }

  @Test
  void testRegisterEntry_UnknownLocationIsRejected() {
    when(locationRepository.findById(LOCATION_ID)).thenReturn(Mono.empty());

    StepVerifier.create(accessControlService.registerEntry(SECURITY_USER_ID, movement()))
        .expectErrorMatches(error -> error instanceof NotFoundException
            && Constants.ERROR_LOCATION_NOT_FOUND.equals(error.getMessage()))
        .verify();
  }

  @Test
  void testRegisterExit_ClosesOpenRecordAndPublishesAvailability() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(authorized()));
    when(recordRepository.findByIdVehicleAndDepartureDateIsNull(VEHICLE_ID))
        .thenReturn(Mono.just(openRecord(LOCATION_ID)));
    when(recordRepository.closeRecord(eq(100), any(), any())).thenReturn(Mono.just(1L));
    mockAvailability(availability(42, 33));
    when(availabilityEventPublisher.publishAvailabilityChanged(any())).thenReturn(Mono.empty());

    StepVerifier.create(accessControlService.registerExit(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertTrue(result.getAuthorized());
          assertEquals(AccessControlResult.EXIT_REGISTERED, result.getResult());
          assertEquals(Constants.MESSAGE_EXIT_REGISTERED, result.getMessage());
          assertNotNull(result.getRecord().getDepartureDate());
          assertEquals(33, result.getAvailability().getAvailable());
        })
        .verifyComplete();

    verify(availabilityEventPublisher, times(1)).publishAvailabilityChanged(any());
  }

  @Test
  void testRegisterExit_WithoutOpenRecordIsRejected() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(authorized()));
    when(recordRepository.findByIdVehicleAndDepartureDateIsNull(VEHICLE_ID))
        .thenReturn(Mono.empty());
    mockAvailability(availability(43, 32));

    StepVerifier.create(accessControlService.registerExit(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertFalse(result.getAuthorized());
          assertEquals(AccessControlResult.NO_OPEN_RECORD, result.getResult());
          assertEquals(Constants.MESSAGE_NO_OPEN_RECORD, result.getMessage());
        })
        .verifyComplete();

    verify(availabilityEventPublisher, never()).publishAvailabilityChanged(any());
  }

  @Test
  void testRegisterExit_OpenRecordOfAnotherLocationIsRejected() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(authorized()));
    when(recordRepository.findByIdVehicleAndDepartureDateIsNull(VEHICLE_ID))
        .thenReturn(Mono.just(openRecord(2)));
    mockAvailability(availability(43, 32));

    StepVerifier.create(accessControlService.registerExit(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertFalse(result.getAuthorized());
          assertEquals(AccessControlResult.RECORD_AT_OTHER_LOCATION, result.getResult());
        })
        .verifyComplete();

    verify(recordRepository, never()).closeRecord(any(), any(), any());
  }

  @Test
  void testRegisterExit_VehicleWithExpiredAuthorizationCanStillLeave() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(
            rejected(ParkingAuthorizationResult.REQUEST_NOT_APPROVED, VEHICLE_ID)));
    when(recordRepository.findByIdVehicleAndDepartureDateIsNull(VEHICLE_ID))
        .thenReturn(Mono.just(openRecord(LOCATION_ID)));
    when(recordRepository.closeRecord(eq(100), any(), any())).thenReturn(Mono.just(1L));
    mockAvailability(availability(42, 33));
    when(availabilityEventPublisher.publishAvailabilityChanged(any())).thenReturn(Mono.empty());

    StepVerifier.create(accessControlService.registerExit(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertTrue(result.getAuthorized());
          assertEquals(AccessControlResult.EXIT_REGISTERED, result.getResult());
        })
        .verifyComplete();
  }

  @Test
  void testRegisterExit_UnknownVehicleIsRejected() {
    mockLocationFound(75);
    when(parkingRequestServiceClient.getParkingAuthorization(NUMBER_PLATE))
        .thenReturn(Mono.just(rejected(ParkingAuthorizationResult.VEHICLE_NOT_FOUND, null)));
    mockAvailability(availability(43, 32));

    StepVerifier.create(accessControlService.registerExit(SECURITY_USER_ID, movement()))
        .assertNext(result -> {
          assertFalse(result.getAuthorized());
          assertEquals(AccessControlResult.VEHICLE_NOT_FOUND, result.getResult());
        })
        .verifyComplete();

    verify(recordRepository, never()).closeRecord(any(), any(), any());
  }
}
