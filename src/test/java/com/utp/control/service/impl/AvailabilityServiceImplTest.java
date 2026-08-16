package com.utp.control.service.impl;

import com.utp.control.client.portal.PortalServiceClient;
import com.utp.control.generated.client.users.model.CampusResponse;
import com.utp.control.model.entity.Location;
import com.utp.control.repository.LocationRepository;
import com.utp.control.repository.RecordRepository;
import com.utp.control.util.Constants;
import com.utp.control.util.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

  private static final Integer LOCATION_AREQUIPA = 1;

  @Mock
  private LocationRepository locationRepository;
  @Mock
  private RecordRepository recordRepository;
  @Mock
  private PortalServiceClient portalServiceClient;

  @InjectMocks
  private AvailabilityServiceImpl availabilityService;

  private Location location(Integer idLocation, String name, Integer capacity) {
    Location location = new Location();
    location.setIdLocation(idLocation);
    location.setIdCampus(1);
    location.setNameLocation(name);
    location.setAddress("Av. Arequipa 265 - 279");
    location.setParkingCapacity(capacity);
    return location;
  }

  private CampusResponse campus() {
    CampusResponse campus = new CampusResponse();
    campus.setIdCampus(1L);
    campus.setNameCampus("Lima Centro");
    return campus;
  }

  @Test
  void testGetAvailability_CalculatesOccupancyFromOpenRecords() {
    when(portalServiceClient.getAllCampus()).thenReturn(Flux.just(campus()));
    when(locationRepository.findAllByOrderByIdLocation())
        .thenReturn(Flux.just(location(LOCATION_AREQUIPA, "Sede Arequipa", 75)));
    when(recordRepository.countByIdLocationAndDepartureDateIsNull(LOCATION_AREQUIPA))
        .thenReturn(Mono.just(43L));

    StepVerifier.create(availabilityService.getAvailability())
        .assertNext(list -> {
          assertEquals(1, list.getAvailability().size());
          var availability = list.getAvailability().getFirst();
          assertEquals(LOCATION_AREQUIPA, availability.getLocationId());
          assertEquals("Sede Arequipa", availability.getLocationName());
          assertEquals("Lima Centro", availability.getCampusName());
          assertEquals(75, availability.getCapacity());
          assertEquals(43, availability.getOccupied());
          assertEquals(32, availability.getAvailable());
        })
        .verifyComplete();
  }

  @Test
  void testGetAvailability_WithoutOpenRecordsExposesFullCapacity() {
    when(portalServiceClient.getAllCampus()).thenReturn(Flux.just(campus()));
    when(locationRepository.findAllByOrderByIdLocation())
        .thenReturn(Flux.just(location(LOCATION_AREQUIPA, "Sede Arequipa", 75)));
    when(recordRepository.countByIdLocationAndDepartureDateIsNull(LOCATION_AREQUIPA))
        .thenReturn(Mono.just(0L));

    StepVerifier.create(availabilityService.getAvailability())
        .assertNext(list -> {
          var availability = list.getAvailability().getFirst();
          assertEquals(0, availability.getOccupied());
          assertEquals(75, availability.getAvailable());
        })
        .verifyComplete();
  }

  @Test
  void testGetAvailability_NeverReturnsNegativeAvailability() {
    when(portalServiceClient.getAllCampus()).thenReturn(Flux.just(campus()));
    when(locationRepository.findAllByOrderByIdLocation())
        .thenReturn(Flux.just(location(LOCATION_AREQUIPA, "Sede Arequipa", 75)));
    when(recordRepository.countByIdLocationAndDepartureDateIsNull(LOCATION_AREQUIPA))
        .thenReturn(Mono.just(80L));

    StepVerifier.create(availabilityService.getAvailability())
        .assertNext(list -> {
          var availability = list.getAvailability().getFirst();
          assertEquals(80, availability.getOccupied());
          assertEquals(0, availability.getAvailable());
        })
        .verifyComplete();
  }

  @Test
  void testGetAvailability_KeepsWorkingWhenCampusNamesAreUnavailable() {
    when(portalServiceClient.getAllCampus())
        .thenReturn(Flux.error(new IllegalStateException("business-core-portal caído")));
    when(locationRepository.findAllByOrderByIdLocation())
        .thenReturn(Flux.just(location(LOCATION_AREQUIPA, "Sede Arequipa", 75)));
    when(recordRepository.countByIdLocationAndDepartureDateIsNull(LOCATION_AREQUIPA))
        .thenReturn(Mono.just(10L));

    StepVerifier.create(availabilityService.getAvailability())
        .assertNext(list -> {
          var availability = list.getAvailability().getFirst();
          assertEquals(65, availability.getAvailable());
          assertEquals(null, availability.getCampusName());
        })
        .verifyComplete();
  }

  @Test
  void testGetLocationAvailability_UnknownLocationIsRejected() {
    when(locationRepository.findById(99)).thenReturn(Mono.empty());

    StepVerifier.create(availabilityService.getLocationAvailability(99))
        .expectErrorMatches(error -> error instanceof NotFoundException
            && Constants.ERROR_LOCATION_NOT_FOUND.equals(error.getMessage()))
        .verify();
  }
}
