package com.utp.control.client.request;

import com.utp.control.generated.model.ParkingAuthorization;
import com.utp.control.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ParkingRequestServiceClient {

  private static final DateTimeFormatter REQUEST_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  private static final String APP_CODE = "P0";

  private final WebClient parkingRequestWebClient;

  public Mono<ParkingAuthorization> getParkingAuthorization(String numberPlate) {
    return parkingRequestWebClient.get()
        .uri("/request/authorization/{numberPlate}", numberPlate)
        .header("Request-ID", UUID.randomUUID().toString())
        .header("request-date", ZonedDateTime.now().format(REQUEST_DATE_FORMAT))
        .header("app-code", APP_CODE)
        .header("caller-name", Constants.NAME_MICROSERVICE)
        .retrieve()
        .bodyToMono(ParkingAuthorization.class);
  }
}
