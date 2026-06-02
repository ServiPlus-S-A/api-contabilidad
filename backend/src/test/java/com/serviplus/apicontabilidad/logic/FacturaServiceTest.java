package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.data.AuditLogRepository;
import com.serviplus.apicontabilidad.data.FacturaRepository;
import com.serviplus.apicontabilidad.domain.EstadoFactura;
import com.serviplus.apicontabilidad.domain.Factura;
import com.serviplus.apicontabilidad.serializer.factura.FacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.FacturaResponse;
import com.serviplus.apicontabilidad.serializer.factura.LineaFacturaRequest;
import com.serviplus.apicontabilidad.utility.NumeroGenerator;
import com.serviplus.apicontabilidad.utility.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("FacturaService — unit tests")
class FacturaServiceTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private NumeroGenerator numeroGenerator;

    private FacturaService facturaService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties(
                new AppProperties.JwtConfig("test-secret-min-32-chars-for-hmac"),
                new AppProperties.IvaConfig(new BigDecimal("0.13")),
                new AppProperties.MinioConfig("http://localhost:9000", "key", "secret", "bucket"),
                new AppProperties.EmailConfig("test@test.com"),
                new AppProperties.CorsConfig("http://localhost:5173")
        );
        facturaService = new FacturaService(
                facturaRepository, auditLogRepository, eventPublisher,
                numeroGenerator, appProperties);
    }

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("debe crear factura con estado PENDIENTE y saldo igual al total")
        void debePersistirConEstadoPendienteYSaldoIgualTotal() {
            // Arrange
            when(numeroGenerator.siguiente("FAC")).thenReturn("FAC-2026-0001");

            LineaFacturaRequest linea = new LineaFacturaRequest(
                    "Servicio", new BigDecimal("1"), new BigDecimal("500.00"));
            FacturaRequest request = new FacturaRequest(
                    1L, "Cliente", LocalDate.now().plusDays(30), null, List.of(linea));

            ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
            Factura fake = facturaStub("FAC-2026-0001", new BigDecimal("500.00"),
                    new BigDecimal("65.00"), new BigDecimal("565.00"));
            when(facturaRepository.save(captor.capture())).thenReturn(fake);

            // Act
            FacturaResponse response = facturaService.crear(request, "user");

            // Assert
            Factura persisted = captor.getValue();
            assertThat(persisted.getEstado()).isEqualTo(EstadoFactura.PENDIENTE);
            assertThat(persisted.getSaldo()).isEqualByComparingTo(persisted.getTotal());
        }

        @Test
        @DisplayName("debe calcular IVA 13% sobre el subtotal")
        void debeCalcularIVACorrectamente() {
            // Arrange
            when(numeroGenerator.siguiente("FAC")).thenReturn("FAC-2026-0002");

            // 2 unidades × 100 = 200 subtotal; IVA 13% = 26; total = 226
            LineaFacturaRequest linea = new LineaFacturaRequest(
                    "Producto", new BigDecimal("2"), new BigDecimal("100.00"));
            FacturaRequest request = new FacturaRequest(
                    1L, "Cliente", LocalDate.now().plusDays(30), null, List.of(linea));

            ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
            when(facturaRepository.save(captor.capture())).thenReturn(
                    facturaStub("FAC-2026-0002", new BigDecimal("200.00"),
                            new BigDecimal("26.00"), new BigDecimal("226.00")));

            // Act
            facturaService.crear(request, "user");

            // Assert
            Factura persisted = captor.getValue();
            assertThat(persisted.getSubtotal()).isEqualByComparingTo("200.00");
            assertThat(persisted.getImpuesto()).isEqualByComparingTo("26.00");
            assertThat(persisted.getTotal()).isEqualByComparingTo("226.00");
        }

        @Test
        @DisplayName("debe publicar FacturaCreadaEvent tras persistir")
        void debePublicarEventoFacturaCreada() {
            // Arrange
            when(numeroGenerator.siguiente("FAC")).thenReturn("FAC-2026-0003");
            LineaFacturaRequest linea = new LineaFacturaRequest(
                    "X", new BigDecimal("1"), new BigDecimal("100.00"));
            FacturaRequest request = new FacturaRequest(
                    1L, "C", LocalDate.now().plusDays(10), null, List.of(linea));
            when(facturaRepository.save(any())).thenReturn(
                    facturaStub("FAC-2026-0003", BigDecimal.TEN, BigDecimal.ONE,
                            new BigDecimal("11.00")));

            // Act
            facturaService.crear(request, "user");

            // Assert
            verify(eventPublisher).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("obtener()")
    class Obtener {

        @Test
        @DisplayName("debe lanzar RecursoNoEncontradoException cuando no existe")
        void debeLanzarExcepcionCuandoNoExiste() {
            // Arrange
            when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> facturaService.obtener(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("99");
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Factura facturaStub(String numero, BigDecimal subtotal,
                                BigDecimal impuesto, BigDecimal total) {
        return Factura.builder()
                .id(1L).numero(numero)
                .clienteId(1L).clienteNombre("Cliente")
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .estado(EstadoFactura.PENDIENTE)
                .subtotal(subtotal).impuesto(impuesto).total(total).saldo(total)
                .creadoEn(LocalDateTime.now()).creadoPor("user")
                .lineas(List.of()).build();
    }
}
