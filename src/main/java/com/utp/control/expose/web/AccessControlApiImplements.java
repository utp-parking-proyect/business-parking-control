package com.utp.control.expose.web;

import com.utp.control.generated.api.ControlApi;
import com.utp.control.generated.model.AccessControlIn;
import com.utp.control.generated.model.AccessControlOut;
import com.utp.control.generated.model.LocationAvailability;
import com.utp.control.generated.model.ParkingAvailabilityList;
import com.utp.control.service.AccessControlService;
import com.utp.control.service.AvailabilityService;
import com.utp.control.util.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@Slf4j
@RequiredArgsConstructor
public class AccessControlApiImplements implements ControlApi {

  private final AccessControlService accessControlService;
  private final AvailabilityService availabilityService;
  private final AuthenticatedUserProvider authenticatedUserProvider;

  @Override
  public Mono<ResponseEntity<AccessControlOut>> registerParkingEntry(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Mono<AccessControlIn> accessControlIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), accessControlIn)
        .flatMap(tuple -> accessControlService.registerEntry(tuple.getT1(), tuple.getT2()))
        .map(result -> ResponseEntity.ok()
            .header("Request-ID", requestID)
            .header("request-date", requestDate)
            .header("app-code", appCode)
            .header("caller-name", callerName)
            .body(result));
  }

  @Override
  public Mono<ResponseEntity<AccessControlOut>> registerParkingExit(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Mono<AccessControlIn> accessControlIn,
      ServerWebExchange exchange) {

    return Mono.zip(authenticatedUserProvider.getAuthenticatedUserId(), accessControlIn)
        .flatMap(tuple -> accessControlService.registerExit(tuple.getT1(), tuple.getT2()))
        .map(result -> ResponseEntity.ok()
            .header("Request-ID", requestID)
            .header("request-date", requestDate)
            .header("app-code", appCode)
            .header("caller-name", callerName)
            .body(result));
  }

  @Override
  public Mono<ResponseEntity<ParkingAvailabilityList>> getParkingAvailability(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      ServerWebExchange exchange) {

    return availabilityService.getAvailability().map(ResponseEntity::ok);
  }

  @Override
  public Mono<ResponseEntity<LocationAvailability>> getLocationAvailability(
      String requestID,
      String requestDate,
      String appCode,
      String callerName,
      Integer locationId,
      ServerWebExchange exchange) {

    return availabilityService.getLocationAvailability(locationId).map(ResponseEntity::ok);
  }
}
