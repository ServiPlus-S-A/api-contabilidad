package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.async.event.CotizacionAprobadaEvent;
import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.data.AuditLogRepository;
import com.serviplus.apicontabilidad.data.CotizacionRepository;
import com.serviplus.apicontabilidad.domain.Cotizacion;
import com.serviplus.apicontabilidad.domain.EstadoCotizacion;
import com.serviplus.apicontabilidad.serializer.cotizacion.CotizacionRequest;
import com.serviplus.apicontabilidad.serializer.cotizacion.CotizacionResponse;
import com.serviplus.apicontabilidad.serializer.cotizacion.LineaCotizacionRequest;
import com.serviplus.apicontabilidad.utility.NumeroGenerator;
import com.serviplus.apicontabilidad.utility.RecursoNoEncontradoException;
import com.serviplus.apicontabilidad.utility.TransicionInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CotizacionService — unit tests")
class CotizacionServiceTest {

    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NumeroGenerator numeroGenerator;

    @InjectMocks private CotizacionService cotizacionService;

    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties(
                new AppProperties.JwtConfig("test-secret-min-32-chars-for-hmac"),
                new AppProperties.IvaConfig(new BigDecimal("0.13")),
                new AppProperties.MinioConfig("http://localhost:9000", "key", "secret", "bucket"),
                new AppProperties.EmailConfig("test@test.com"),
                new AppProperties.CorsConfig("http://localhost:5173"),
                "http://localhost:8000"
        );
        // Manually inject — @InjectMocks works on constructor but AppProperties
        // uses a real instance, so we use constructor injection via reflection or
        // reconstruct with a partial mock. We use the real bean here.
        cotizacionService = new CotizacionService(
                cotizacionRepository, auditLogRepository, eventPublisher,
                numeroGenerator, appProperties);
    }

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("debe calcular subtotal, impuesto 13% y total correctamente")
        void debeCalcularTotalesCorrectamente() {
            // Arrange
            when(numeroGenerator.siguiente("COT")).thenReturn("COT-2026-0001");

            LineaCotizacionRequest linea = new LineaCotizacionRequest(
                    "Servicio A", new BigDecimal("2"), new BigDecimal("100.00"));
            CotizacionRequest request = new CotizacionRequest(
                    1L, "Cliente Test", LocalDate.now().plusDays(30),
                    null, List.of(linea));

            Cotizacion saved = Cotizacion.builder()
                    .id(1L).numero("COT-2026-0001")
                    .clienteId(1L).clienteNombre("Cliente Test")
                    .fechaVigencia(LocalDate.now().plusDays(30))
                    .estado(EstadoCotizacion.BORRADOR)
                    .subtotal(new BigDecimal("200.00"))
                    .impuesto(new BigDecimal("26.00"))
                    .total(new BigDecimal("226.00"))
                    .creadoEn(LocalDateTime.now()).creadoPor("user1")
                    .lineas(List.of()).build();
            when(cotizacionRepository.save(any())).thenReturn(saved);

            // Act
            CotizacionResponse response = cotizacionService.crear(request, "user1");

            // Assert
            assertThat(response.subtotal()).isEqualByComparingTo("200.00");
            assertThat(response.impuesto()).isEqualByComparingTo("26.00");
            assertThat(response.total()).isEqualByComparingTo("226.00");
            assertThat(response.estado()).isEqualTo(EstadoCotizacion.BORRADOR);
        }

        @Test
        @DisplayName("debe guardar con estado BORRADOR")
        void debeGuardarConEstadoBorrador() {
            // Arrange
            when(numeroGenerator.siguiente("COT")).thenReturn("COT-2026-0002");
            LineaCotizacionRequest linea = new LineaCotizacionRequest(
                    "Item", new BigDecimal("1"), new BigDecimal("50.00"));
            CotizacionRequest request = new CotizacionRequest(
                    2L, "Cliente B", LocalDate.now().plusDays(10), null, List.of(linea));

            ArgumentCaptor<Cotizacion> captor = ArgumentCaptor.forClass(Cotizacion.class);
            Cotizacion fakeReturn = Cotizacion.builder().id(2L).numero("COT-2026-0002")
                    .clienteId(2L).clienteNombre("Cliente B")
                    .fechaVigencia(LocalDate.now().plusDays(10))
                    .estado(EstadoCotizacion.BORRADOR)
                    .subtotal(BigDecimal.ZERO).impuesto(BigDecimal.ZERO).total(BigDecimal.ZERO)
                    .creadoEn(LocalDateTime.now()).creadoPor("user2").lineas(List.of()).build();
            when(cotizacionRepository.save(captor.capture())).thenReturn(fakeReturn);

            // Act
            cotizacionService.crear(request, "user2");

            // Assert
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoCotizacion.BORRADOR);
            assertThat(captor.getValue().getCreadoPor()).isEqualTo("user2");
        }

        @Test
        @DisplayName("debe calcular subtotal por línea = cantidad × precioUnitario")
        void debeCalcularSubtotalPorLinea() {
            // Arrange
            when(numeroGenerator.siguiente("COT")).thenReturn("COT-2026-0003");
            LineaCotizacionRequest l1 = new LineaCotizacionRequest(
                    "Item A", new BigDecimal("3"), new BigDecimal("50.00")); // 150
            LineaCotizacionRequest l2 = new LineaCotizacionRequest(
                    "Item B", new BigDecimal("2"), new BigDecimal("75.00")); // 150
            CotizacionRequest request = new CotizacionRequest(
                    1L, "Cliente", LocalDate.now().plusDays(10), null, List.of(l1, l2));

            ArgumentCaptor<Cotizacion> captor = ArgumentCaptor.forClass(Cotizacion.class);
            Cotizacion fakeReturn = Cotizacion.builder().id(3L).numero("COT-2026-0003")
                    .clienteId(1L).clienteNombre("Cliente")
                    .fechaVigencia(LocalDate.now().plusDays(10))
                    .estado(EstadoCotizacion.BORRADOR)
                    .subtotal(new BigDecimal("300.00"))
                    .impuesto(new BigDecimal("39.00"))
                    .total(new BigDecimal("339.00"))
                    .creadoEn(LocalDateTime.now()).creadoPor("u")
                    .lineas(List.of()).build();
            when(cotizacionRepository.save(captor.capture())).thenReturn(fakeReturn);

            // Act
            cotizacionService.crear(request, "u");

            // Assert — subtotal = 150 + 150 = 300
            assertThat(captor.getValue().getSubtotal()).isEqualByComparingTo("300.00");
            assertThat(captor.getValue().getImpuesto()).isEqualByComparingTo("39.00");
        }
    }

    @Nested
    @DisplayName("aprobar()")
    class Aprobar {

        @Test
        @DisplayName("debe transicionar de ENVIADA a ACEPTADA y publicar evento")
        void debeAprobarCuandoEnviada() {
            // Arrange
            Cotizacion cotizacion = cotizacionConEstado(EstadoCotizacion.ENVIADA);
            when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cotizacion));
            when(cotizacionRepository.save(any())).thenReturn(cotizacion);

            // Act
            CotizacionResponse response = cotizacionService.aprobar(1L, "admin");

            // Assert
            assertThat(response.estado()).isEqualTo(EstadoCotizacion.ACEPTADA);
            verify(eventPublisher).publishEvent(any(CotizacionAprobadaEvent.class));
            verify(auditLogRepository).save(any());
        }

        @Test
        @DisplayName("debe lanzar TransicionInvalidaException si ya está ACEPTADA")
        void debeLanzarExcepcionSiYaAceptada() {
            // Arrange
            Cotizacion cotizacion = cotizacionConEstado(EstadoCotizacion.ACEPTADA);
            when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cotizacion));

            // Act & Assert
            assertThatThrownBy(() -> cotizacionService.aprobar(1L, "admin"))
                    .isInstanceOf(TransicionInvalidaException.class)
                    .hasMessageContaining("ACEPTADA");
        }

        @Test
        @DisplayName("debe lanzar RecursoNoEncontradoException si no existe")
        void debeLanzarExcepcionSiNoExiste() {
            // Arrange
            when(cotizacionRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> cotizacionService.aprobar(99L, "admin"))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("rechazar()")
    class Rechazar {

        @Test
        @DisplayName("debe transicionar de ENVIADA a RECHAZADA")
        void debeRechazarCuandoEnviada() {
            // Arrange
            Cotizacion cotizacion = cotizacionConEstado(EstadoCotizacion.ENVIADA);
            when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cotizacion));
            when(cotizacionRepository.save(any())).thenReturn(cotizacion);

            // Act
            CotizacionResponse response = cotizacionService.rechazar(1L, "admin");

            // Assert
            assertThat(response.estado()).isEqualTo(EstadoCotizacion.RECHAZADA);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("debe lanzar TransicionInvalidaException si está en BORRADOR")
        void debeLanzarExcepcionSiBorrador() {
            // Arrange
            Cotizacion cotizacion = cotizacionConEstado(EstadoCotizacion.BORRADOR);
            when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cotizacion));

            // Act & Assert
            assertThatThrownBy(() -> cotizacionService.rechazar(1L, "admin"))
                    .isInstanceOf(TransicionInvalidaException.class);
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Cotizacion cotizacionConEstado(EstadoCotizacion estado) {
        return Cotizacion.builder()
                .id(1L).numero("COT-2026-0001")
                .clienteId(1L).clienteNombre("Test")
                .fechaVigencia(LocalDate.now().plusDays(30))
                .estado(estado)
                .subtotal(new BigDecimal("100.00"))
                .impuesto(new BigDecimal("13.00"))
                .total(new BigDecimal("113.00"))
                .creadoEn(LocalDateTime.now()).creadoPor("user")
                .lineas(List.of()).build();
    }
}
