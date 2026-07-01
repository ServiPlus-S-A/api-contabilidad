package com.serviplus.apicontabilidad.integration;

import com.serviplus.apicontabilidad.domain.EstadoFactura;
import com.serviplus.apicontabilidad.serializer.factura.AnularFacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.FacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.FacturaResponse;
import com.serviplus.apicontabilidad.serializer.factura.LineaFacturaRequest;
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
@DisplayName("Factura API — integration tests")
class FacturaIT extends AbstractContainerIT {

    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = makeClient();
    }

    @MockitoBean
    private MinioClient minioClient;

    @MockitoBean
    private JavaMailSender mailSender;

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private FacturaRequest buildRequest() {
        return new FacturaRequest(
                1L,
                "Cliente Factura S.A.",
                LocalDate.now().plusDays(30),
                null,
                List.of(new LineaFacturaRequest(
                        "Servicio de desarrollo",
                        new BigDecimal("1.00"),
                        new BigDecimal("1000.00")))
        );
    }

    // ── POST /api/v1/facturas ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/facturas")
    class Crear {

        @Test
        @DisplayName("201 con número, estado PENDIENTE y saldo igual al total")
        void debeCrearFacturaYRetornar201() {
            HttpEntity<FacturaRequest> req = new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.contadorToken()));

            ResponseEntity<FacturaResponse> res = restTemplate.postForEntity(
                    url("/api/v1/facturas"), req, FacturaResponse.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            FacturaResponse body = res.getBody();
            assertThat(body).isNotNull();
            assertThat(body.numero()).startsWith("FAC-");
            assertThat(body.estado()).isEqualTo(EstadoFactura.PENDIENTE);
            assertThat(body.saldo()).isEqualByComparingTo(body.total());
        }

        @Test
        @DisplayName("calcula correctamente: 1×1000=1000 subtotal, 130 IVA, 1130 total")
        void debeCalcularTotales() {
            HttpEntity<FacturaRequest> req = new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.contadorToken()));

            ResponseEntity<FacturaResponse> res = restTemplate.postForEntity(
                    url("/api/v1/facturas"), req, FacturaResponse.class);

            FacturaResponse body = res.getBody();
            assertThat(body).isNotNull();
            assertThat(body.subtotal()).isEqualByComparingTo("1000.00");
            assertThat(body.impuesto()).isEqualByComparingTo("130.00");
            assertThat(body.total()).isEqualByComparingTo("1130.00");
        }
    }

    // ── GET /api/v1/facturas/{id} ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/facturas/{id}")
    class Obtener {

        @Test
        @DisplayName("200 con datos completos de la factura recién creada")
        void debeRetornarFacturaPorId() {
            ResponseEntity<FacturaResponse> created = restTemplate.postForEntity(
                    url("/api/v1/facturas"),
                    new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.contadorToken())),
                    FacturaResponse.class);
            long id = created.getBody().id();

            ResponseEntity<FacturaResponse> res = restTemplate.exchange(
                    url("/api/v1/facturas/" + id), HttpMethod.GET,
                    new HttpEntity<>(authHeaders(JwtTestHelper.contadorToken())),
                    FacturaResponse.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().id()).isEqualTo(id);
            assertThat(res.getBody().clienteNombre()).isEqualTo("Cliente Factura S.A.");
        }

        @Test
        @DisplayName("404 cuando la factura no existe")
        void debeRetornar404CuandoNoExiste() {
            ResponseEntity<String> res = restTemplate.exchange(
                    url("/api/v1/facturas/999999"), HttpMethod.GET,
                    new HttpEntity<>(authHeaders(JwtTestHelper.contadorToken())),
                    String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ── PUT /api/v1/facturas/{id}/anular ─────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/facturas/{id}/anular")
    class Anular {

        private long crearFactura() {
            ResponseEntity<FacturaResponse> res = restTemplate.postForEntity(
                    url("/api/v1/facturas"),
                    new HttpEntity<>(buildRequest(), authHeaders(JwtTestHelper.contadorToken())),
                    FacturaResponse.class);
            return res.getBody().id();
        }

        @Test
        @DisplayName("200 y estado ANULADA al anular una factura PENDIENTE")
        void debeAnularFacturaPendiente() {
            long id = crearFactura();
            AnularFacturaRequest body = new AnularFacturaRequest("Error de cobro detectado en factura");

            ResponseEntity<FacturaResponse> res = restTemplate.exchange(
                    url("/api/v1/facturas/" + id + "/anular"),
                    HttpMethod.PUT,
                    new HttpEntity<>(body, authHeaders(JwtTestHelper.adminToken())),
                    FacturaResponse.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(res.getBody().estado()).isEqualTo(EstadoFactura.ANULADA);
        }

        @Test
        @DisplayName("400 cuando el motivo está vacío")
        void debeRetornar400SiMotivoVacio() {
            long id = crearFactura();
            AnularFacturaRequest body = new AnularFacturaRequest("");

            ResponseEntity<String> res = restTemplate.exchange(
                    url("/api/v1/facturas/" + id + "/anular"),
                    HttpMethod.PUT,
                    new HttpEntity<>(body, authHeaders(JwtTestHelper.adminToken())),
                    String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("400 cuando el motivo tiene menos de 10 caracteres")
        void debeRetornar400SiMotivoDemasiadoCorto() {
            long id = crearFactura();
            AnularFacturaRequest body = new AnularFacturaRequest("corto");

            ResponseEntity<String> res = restTemplate.exchange(
                    url("/api/v1/facturas/" + id + "/anular"),
                    HttpMethod.PUT,
                    new HttpEntity<>(body, authHeaders(JwtTestHelper.adminToken())),
                    String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("403 cuando ROLE_CLIENTE intenta anular")
        void debeRetornar403ParaRolCliente() {
            long id = crearFactura();
            AnularFacturaRequest body = new AnularFacturaRequest("Motivo válido de anulación aquí");

            ResponseEntity<String> res = restTemplate.exchange(
                    url("/api/v1/facturas/" + id + "/anular"),
                    HttpMethod.PUT,
                    new HttpEntity<>(body, authHeaders(JwtTestHelper.clienteToken())),
                    String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("422 al intentar anular una factura ya ANULADA")
        void debeRetornar422SiYaAnulada() {
            long id = crearFactura();
            AnularFacturaRequest body = new AnularFacturaRequest("Primer motivo de anulación válido");

            restTemplate.exchange(url("/api/v1/facturas/" + id + "/anular"),
                    HttpMethod.PUT, new HttpEntity<>(body, authHeaders(JwtTestHelper.adminToken())),
                    FacturaResponse.class);

            ResponseEntity<String> res = restTemplate.exchange(
                    url("/api/v1/facturas/" + id + "/anular"),
                    HttpMethod.PUT,
                    new HttpEntity<>(body, authHeaders(JwtTestHelper.adminToken())),
                    String.class);

            assertThat(res.getStatusCode().value()).isEqualTo(422);
        }
    }
}
