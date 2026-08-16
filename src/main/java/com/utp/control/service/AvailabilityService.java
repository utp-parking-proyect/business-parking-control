package com.utp.control.service;

import com.utp.control.generated.model.LocationAvailability;
import com.utp.control.generated.model.ParkingAvailabilityList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AvailabilityService {

  Mono<ParkingAvailabilityList> getAvailability();

  Flux<LocationAvailability> getAvailabilityStream();

  Mono<LocationAvailability> getLocationAvailability(Integer locationId);
}
