package com.serviplus.apicontabilidad.integration;

import com.serviplus.apicontabilidad.domain.EstadoCotizacion;
import com.serviplus.apicontabilidad.serializer.cotizacion.CotizacionRequest;
import com.serviplus.apicontabilidad.serializer.cotizacion.CotizacionResponse;
import com.serviplus.apicontabilidad.serializer.cotizacion.LineaCotizacionRequest;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("Cotización API — integration tests")
class CotizacionIT extends AbstractContainerIT {

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = makeClient();
    }

    @MockitoBean
    private MinioClient minioClient;

    @MockitoBean
    private JavaMailSender mailSender;

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private CotizacionRequest buildRequest() {
        return new CotizacionRequest(
                1L,
                "Cliente Integración S.A.",
                LocalDate.now().plusDays(30),
                "Notas de prueba",
                List.of(new LineaCotizacionRequest(
                        "Consultoría técnica",
                        new BigDecimal("2.00"),
                        new BigDecimal("500.00")))
        );
    }

    // ── POST /api/v1/cotizaciones ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/cotizaciones")
    class Crear {

        @Test
        @DisplayName("201 con body correcto al crear cotización")
        void debeCrearCotizacionYRetornar201() {
            HttpEntity<CotizacionRequest> req = new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.clienteToken()));

            ResponseEntity<CotizacionResponse> res = restTemplate.postForEntity(
                    url("/api/v1/cotizaciones"), req, CotizacionResponse.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().numero()).startsWith("COT-");
            assertThat(res.getBody().estado()).isEqualTo(EstadoCotizacion.BORRADOR);
            assertThat(res.getBody().clienteNombre()).isEqualTo("Cliente Integración S.A.");
        }

        @Test
        @DisplayName("calcula IVA 13% — 2×500=1000 subtotal, 130 impuesto, 1130 total")
        void debeCalcularTotalesConIVA13() {
            HttpEntity<CotizacionRequest> req = new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.clienteToken()));

            ResponseEntity<CotizacionResponse> res = restTemplate.postForEntity(
                    url("/api/v1/cotizaciones"), req, CotizacionResponse.class);

            CotizacionResponse body = res.getBody();
            assertThat(body).isNotNull();
            assertThat(body.subtotal()).isEqualByComparingTo("1000.00");
            assertThat(body.impuesto()).isEqualByComparingTo("130.00");
            assertThat(body.total()).isEqualByComparingTo("1130.00");
        }

        @Test
        @DisplayName("401 sin token de autenticación")
        void debeRetornar401SinToken() {
            ResponseEntity<String> res = restTemplate.postForEntity(
                    url("/api/v1/cotizaciones"), new HttpEntity<>(buildRequest()), String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("400 con body inválido — lista de líneas vacía")
        void debeRetornar400ConBodyInvalido() {
            CotizacionRequest invalida = new CotizacionRequest(
                    1L, "Cliente", LocalDate.now().plusDays(10), null, List.of());
            HttpEntity<CotizacionRequest> req = new HttpEntity<>(invalida, authHeaders(JwtTestHelper.clienteToken()));

            ResponseEntity<String> res = restTemplate.postForEntity(
                    url("/api/v1/cotizaciones"), req, String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── GET /api/v1/cotizaciones ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/cotizaciones")
    class Listar {

        @Test
        @DisplayName("200 con lista vacía cuando no hay registros")
        void debeRetornarListaVacia() {
            ResponseEntity<CotizacionResponse[]> res = restTemplate.exchange(
                    url("/api/v1/cotizaciones"), HttpMethod.GET,
                    new HttpEntity<>(authHeaders(JwtTestHelper.clienteToken())),
                    CotizacionResponse[].class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(res.getBody()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("retorna la cotización recién creada")
        void debeRetornarCotizacionCreada() {
            restTemplate.postForEntity(url("/api/v1/cotizaciones"),
                    new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.clienteToken())),
                    CotizacionResponse.class);

            ResponseEntity<CotizacionResponse[]> res = restTemplate.exchange(
                    url("/api/v1/cotizaciones"), HttpMethod.GET,
                    new HttpEntity<>(authHeaders(JwtTestHelper.clienteToken())),
                    CotizacionResponse[].class);

            assertThat(res.getBody()).hasSize(1);
        }
    }

    // ── PUT /api/v1/cotizaciones/{id}/aprobar ─────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/cotizaciones/{id}/aprobar")
    class Aprobar {

        @Test
        @DisplayName("422 al aprobar desde BORRADOR — transición inválida según State Pattern")
        void debeRetornar422SiEstaBorrador() {
            ResponseEntity<CotizacionResponse> created = restTemplate.postForEntity(
                    url("/api/v1/cotizaciones"),
                    new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.clienteToken())),
                    CotizacionResponse.class);
            long id = created.getBody().id();

            ResponseEntity<String> res = restTemplate.exchange(
                    url("/api/v1/cotizaciones/" + id + "/aprobar"),
                    HttpMethod.PUT,
                    new HttpEntity<>(authHeaders(JwtTestHelper.adminToken())),
                    String.class);

            // HttpStatus.UNPROCESSABLE_ENTITY deprecated in Spring Framework 7 — compare by value
            assertThat(res.getStatusCode().value()).isEqualTo(422);
        }

        @Test
        @DisplayName("403 cuando ROLE_CLIENTE intenta aprobar (requiere ADMIN o CONTADOR)")
        void debeRetornar403ParaRolCliente() {
            ResponseEntity<CotizacionResponse> created = restTemplate.postForEntity(
                    url("/api/v1/cotizaciones"),
                    new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.clienteToken())),
                    CotizacionResponse.class);
            long id = created.getBody().id();

            ResponseEntity<String> res = restTemplate.exchange(
                    url("/api/v1/cotizaciones/" + id + "/aprobar"),
                    HttpMethod.PUT,
                    new HttpEntity<>(authHeaders(JwtTestHelper.clienteToken())),
                    String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
