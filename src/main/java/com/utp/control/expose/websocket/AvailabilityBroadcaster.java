package com.utp.control.expose.websocket;

import com.utp.control.generated.model.LocationAvailability;
import com.utp.control.service.AvailabilityEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@Slf4j
public class AvailabilityBroadcaster implements AvailabilityEventPublisher {

  private final Sinks.Many<LocationAvailability> sink =
      Sinks.many().multicast().directBestEffort();

  @Override
  public Mono<Void> publishAvailabilityChanged(LocationAvailability availability) {
    return Mono.fromRunnable(() -> {
      Sinks.EmitResult result = sink.tryEmitNext(availability);
      if (result.isFailure()) {
        log.warn("No se pudo publicar la disponibilidad de la sede {}: {}",
            availability.getLocationId(), result);
      }
    });
  }

  public Flux<LocationAvailability> availabilityChanges() {
    return sink.asFlux();
  }
}
