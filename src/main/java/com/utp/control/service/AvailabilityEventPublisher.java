package com.utp.control.service;

import com.utp.control.generated.model.LocationAvailability;
import reactor.core.publisher.Mono;

public interface AvailabilityEventPublisher {

  Mono<Void> publishAvailabilityChanged(LocationAvailability availability);
}
