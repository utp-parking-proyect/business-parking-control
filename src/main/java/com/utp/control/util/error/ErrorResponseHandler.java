package com.utp.control.util.error;

import com.utp.control.generated.model.ApiExceptionDetail;
import com.utp.control.generated.model.ModelApiException;
import com.utp.control.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.MissingRequestValueException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;

@Component
@RestControllerAdvice
@Slf4j
public class ErrorResponseHandler {

  private static final String ERROR_TYPE_FUNCTIONAL = "FUNCTIONAL";
  private static final String ERROR_TYPE_TECHNICAL = "TECHNICAL";

  @ExceptionHandler(IllegalArgumentException.class)
  public Mono<ResponseEntity<ModelApiException>> handleValidationError(IllegalArgumentException e) {
    log.error("Error de validación: {}", e.getMessage());
    ModelApiException errorResponse = new ModelApiException();
    errorResponse.setDescription(e.getMessage());
    errorResponse.setErrorType(ERROR_TYPE_FUNCTIONAL);

    buildApiExceptionDetail(e.getMessage(), errorResponse);

    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
  }

  @ExceptionHandler(NotFoundException.class)
  public Mono<ResponseEntity<ModelApiException>> handleNotFound(NotFoundException e) {
    log.error("No encontrado: {}", e.getMessage());
    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(buildFunctionalError(e.getMessage())));
  }

  @ExceptionHandler(ConflictException.class)
  public Mono<ResponseEntity<ModelApiException>> handleConflict(ConflictException e) {
    log.error("Conflicto: {}", e.getMessage());
    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
        .body(buildFunctionalError(e.getMessage())));
  }

  @ExceptionHandler(ForbiddenException.class)
  public Mono<ResponseEntity<ModelApiException>> handleForbidden(ForbiddenException e) {
    log.error("Prohibido: {}", e.getMessage());
    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(buildFunctionalError(e.getMessage())));
  }

  @ExceptionHandler({WebClientResponseException.class, WebClientRequestException.class})
  public Mono<ResponseEntity<ModelApiException>> handleServiceUnavailable(Exception e) {
    log.error("Falló la llamada a un microservicio externo: {}", e.getMessage());
    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(buildFunctionalError(Constants.ERROR_REQUEST_SERVICE_UNAVAILABLE)));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ModelApiException>> handleUnexpectedError(Exception e) {
    log.error("Error inesperado", e);
    ModelApiException errorResponse = new ModelApiException();
    errorResponse.setDescription("Ocurrió un error inesperado");
    errorResponse.setErrorType(ERROR_TYPE_TECHNICAL);

    buildApiExceptionDetail(e.getMessage(), errorResponse);

    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
  }

  @ExceptionHandler(MissingRequestValueException.class)
  public Mono<ResponseEntity<ModelApiException>> handleMissingRequestValue(
      MissingRequestValueException e) {
    log.error("Falta un valor requerido en la petición: {}", e.getMessage());

    String headerName = extractHeaderName(e.getMessage());

    ModelApiException errorResponse = new ModelApiException();
    errorResponse.setDescription("Header requerido faltante: " + headerName);
    errorResponse.setErrorType(ERROR_TYPE_FUNCTIONAL);

    buildApiExceptionDetail(
        "El header '" + headerName + "' es requerido pero no fue proporcionado", errorResponse);

    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
  }

  private ModelApiException buildFunctionalError(String message) {
    ModelApiException errorResponse = new ModelApiException();
    errorResponse.setDescription(message);
    errorResponse.setErrorType(ERROR_TYPE_FUNCTIONAL);
    buildApiExceptionDetail(message, errorResponse);
    return errorResponse;
  }

  private void buildApiExceptionDetail(String description, ModelApiException errorResponse) {
    ArrayList<ApiExceptionDetail> details = new ArrayList<>();
    ApiExceptionDetail detail = new ApiExceptionDetail();
    detail.setComponent(Constants.NAME_MICROSERVICE);
    detail.setDescription(description);
    details.add(detail);

    errorResponse.setExceptionDetails(details);
    errorResponse.setProperties(new HashMap<>());
  }

  private String extractHeaderName(String message) {
    int startIndex = message.indexOf("'") + 1;
    int endIndex = message.lastIndexOf("'");
    if (startIndex > 0 && endIndex > startIndex) {
      return message.substring(startIndex, endIndex);
    }
    return "Header desconocido";
  }
}
