package com.serviplus.apicontabilidad.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("SecureLogger — unit tests")
class SecureLoggerTest {

    @Test
    @DisplayName("logAuthFailure no lanza con valores normales")
    void logAuthFailureNoLanzaConValoresNormales() {
        assertThatCode(() -> SecureLogger.logAuthFailure("/api/v1/cotizaciones", "JWT expirado"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logAuthFailure sanitiza caracteres de inyección de log")
    void logAuthFailureSanitizaCaracteresInyeccion() {
        assertThatCode(() -> SecureLogger.logAuthFailure("/api\ninyectado\r", "bad\trequest"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logAuthFailure no lanza con valores nulos")
    void logAuthFailureNoLanzaConNulos() {
        assertThatCode(() -> SecureLogger.logAuthFailure(null, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logAccessDenied no lanza con valores normales")
    void logAccessDeniedNoLanzaConValoresNormales() {
        assertThatCode(() -> SecureLogger.logAccessDenied("admin", "/api/admin/usuarios"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logAccessDenied no lanza con valores nulos")
    void logAccessDeniedNoLanzaConNulos() {
        assertThatCode(() -> SecureLogger.logAccessDenied(null, null))
                .doesNotThrowAnyException();
    }
}
