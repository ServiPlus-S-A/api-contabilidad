package com.serviplus.apicontabilidad.integration;

import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the full Spring application context loads and the actuator health
 * endpoint responds — replaces the trivial BackendApplicationTests context-loads test
 * with a real check against a running database (Testcontainers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Application context — integration smoke test")
class ApplicationContextIT extends AbstractContainerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private MinioClient minioClient;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    @DisplayName("contexto carga y /actuator/health responde UP")
    void actuatorHealthDebeResponderUp() {
        ResponseEntity<String> res = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"status\":\"UP\"");
    }
}
