package com.utp.control.util.error;

import com.utp.control.generated.model.ModelApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.MissingRequestValueException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorResponseHandlerTest {

  @InjectMocks
  private ErrorResponseHandler errorResponseHandler;

  @Mock
  private MissingRequestValueException mockException;

  @Test
  void testHandleValidationError() {
    // Arrange
    IllegalArgumentException exception = new IllegalArgumentException("Datos inválidos");

    // Act & Assert
    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleValidationError(exception);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.BAD_REQUEST;
          assert response.getBody() != null;
          assert response.getBody().getDescription().equals("Datos inválidos");
          assert response.getBody().getErrorType().equals("FUNCTIONAL");
        })
        .verifyComplete();
  }

  @Test
  void testHandleUnexpectedError() {
    // Arrange
    Exception exception = new Exception("Ocurrió un error inesperado en el servidor");

    // Act & Assert
    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleUnexpectedError(exception);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
          assert response.getBody() != null;
          assert response.getBody().getDescription().equals("Ocurrió un error inesperado");
          assert response.getBody().getErrorType().equals("TECHNICAL");
        })
        .verifyComplete();
  }

  @Test
  void testHandleMissingRequestValue() {
    // Arrange
    when(mockException.getMessage()).thenReturn("Required header 'caller-name' is not present.");

    // Act & Assert
    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleMissingRequestValue(mockException);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.BAD_REQUEST;
          assert response.getBody() != null;
          assert response.getBody().getErrorType().equals("FUNCTIONAL");
        })
        .verifyComplete();
  }

  @Test
  void testHandleNotFound() {
    NotFoundException exception = new NotFoundException("La sede indicada no existe");

    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleNotFound(exception);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.NOT_FOUND;
          assert response.getBody() != null;
          assert response.getBody().getDescription().equals("La sede indicada no existe");
        })
        .verifyComplete();
  }

  @Test
  void testHandleConflict() {
    ConflictException exception = new ConflictException("El ingreso ya fue registrado");

    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleConflict(exception);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.CONFLICT;
          assert response.getBody() != null;
          assert response.getBody().getDescription().equals("El ingreso ya fue registrado");
        })
        .verifyComplete();
  }

  @Test
  void testHandleForbidden() {
    ForbiddenException exception = new ForbiddenException("El token no contiene el identificador del usuario autenticado");

    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleForbidden(exception);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.FORBIDDEN;
          assert response.getBody() != null;
        })
        .verifyComplete();
  }

  @Test
  void testHandleServiceUnavailable() {
    WebClientResponseException exception = WebClientResponseException.create(
        503, "Service Unavailable", HttpHeaders.EMPTY, new byte[0], null);

    Mono<ResponseEntity<ModelApiException>> result = errorResponseHandler.handleServiceUnavailable(exception);

    StepVerifier.create(result)
        .assertNext(response -> {
          assert response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE;
          assert response.getBody() != null;
        })
        .verifyComplete();
  }
}
