package com.utp.control.expose.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.control.generated.model.LocationAvailability;
import com.utp.control.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class AvailabilityWebSocketHandler implements WebSocketHandler {

  private final AvailabilityService availabilityService;
  private final AvailabilityBroadcaster availabilityBroadcaster;
  private final ObjectMapper objectMapper;

  @Override
  @NullMarked
  public Mono<Void> handle(WebSocketSession session) {
    Flux<LocationAvailability> availability = availabilityService.getAvailabilityStream()
        .onErrorResume(error -> {
          log.error("No se pudo enviar la disponibilidad inicial a la sesión {}: {}",
              session.getId(), error.getMessage());
          return Flux.empty();
        })
        .concatWith(availabilityBroadcaster.availabilityChanges());

    Mono<Void> output = session.send(availability
        .mapNotNull(this::serialize)
        .map(session::textMessage));

    Mono<Void> input = session.receive().then();

    return Mono.fromRunnable(() -> log.info("WebSocket de disponibilidad conectado - sesión {}",
            session.getId()))
        .thenMany(Flux.merge(output, input))
        .then()
        .doFinally(signal -> log.info("WebSocket de disponibilidad desconectado - sesión {} ({})",
            session.getId(), signal));
  }

  private String serialize(LocationAvailability availability) {
    try {
      return objectMapper.writeValueAsString(availability);
    } catch (JsonProcessingException e) {
      log.error("No se pudo serializar la disponibilidad de la sede {}: {}",
          availability.getLocationId(), e.getMessage());
      return null;
    }
  }
}
