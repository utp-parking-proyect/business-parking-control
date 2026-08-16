package com.utp.control.client.portal;

import com.utp.control.generated.client.users.model.CampusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PortalServiceClient {

  private final WebClient usersWebClient;

  public Flux<CampusResponse> getAllCampus() {
    return usersWebClient.get()
        .uri("/campus")
        .retrieve()
        .bodyToFlux(CampusResponse.class);
  }

  public Mono<CampusResponse> getCampusById(Long id) {
    return usersWebClient.get()
        .uri("/campus/{id}", id)
        .retrieve()
        .bodyToMono(CampusResponse.class);
  }
}
