package com.serviplus.apicontabilidad.utility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler — unit tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNotFound devuelve 404 con el mensaje original")
    void handleNotFoundDevuelve404() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Cotización no encontrada: 99");

        ResponseEntity<ApiError> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Cotización no encontrada: 99");
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    @DisplayName("handleTransicion devuelve 422")
    void handleTransicionDevuelve422() {
        TransicionInvalidaException ex = new TransicionInvalidaException("Transición inválida: BORRADOR → RECHAZADA");

        ResponseEntity<ApiError> response = handler.handleTransicion(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(422);
    }

    @Test
    @DisplayName("handleDataIntegrity devuelve 409 con mensaje de conflicto")
    void handleDataIntegrityDevuelve409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate entry");

        ResponseEntity<ApiError> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("Conflicto");
    }

    @Test
    @DisplayName("handleReglaNegocio devuelve 400")
    void handleReglaNegocioDevuelve400() {
        ReglaNegocioException ex = new ReglaNegocioException("Monto supera el saldo");

        ResponseEntity<ApiError> response = handler.handleReglaNegocio(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Monto supera el saldo");
    }

    @Test
    @DisplayName("handleValidation devuelve 400 concatenando mensajes de campos")
    void handleValidationDevuelve400ConMensajesDeCampos() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", "clienteNombre", "no puede estar vacío");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("no puede estar vacío");
    }

    @Test
    @DisplayName("handleValidation concatena múltiples errores con punto y coma")
    void handleValidationConcatenaMensajes() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> errors = List.of(
                new FieldError("r", "campo1", "mensaje uno"),
                new FieldError("r", "campo2", "mensaje dos")
        );
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(errors);

        ResponseEntity<ApiError> response = handler.handleValidation(ex);

        assertThat(response.getBody().message()).contains("mensaje uno").contains("mensaje dos");
    }

    @Test
    @DisplayName("handleAccessDenied devuelve 403")
    void handleAccessDeniedDevuelve403() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ApiError> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("Acceso denegado");
    }

    @Test
    @DisplayName("handleGeneral devuelve 500 con mensaje genérico")
    void handleGeneralDevuelve500() {
        Exception ex = new RuntimeException("NullPointerException inesperado");

        ResponseEntity<ApiError> response = handler.handleGeneral(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("Error interno del servidor");
    }
}
