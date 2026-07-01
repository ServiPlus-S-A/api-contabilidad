package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.data.AuditLogRepository;
import com.serviplus.apicontabilidad.data.FacturaRepository;
import com.serviplus.apicontabilidad.domain.EstadoFactura;
import com.serviplus.apicontabilidad.domain.Factura;
import com.serviplus.apicontabilidad.serializer.factura.AnularFacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.FacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.FacturaResponse;
import com.serviplus.apicontabilidad.serializer.factura.LineaFacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.PdfDescarga;
import com.serviplus.apicontabilidad.utility.NumeroGenerator;
import com.serviplus.apicontabilidad.utility.RecursoNoEncontradoException;
import com.serviplus.apicontabilidad.utility.TransicionInvalidaException;
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
                new AppProperties.JwtConfig("test-secret-min-32-chars-for-hmac", 86400000L),
                new AppProperties.IvaConfig(new BigDecimal("0.13")),
                new AppProperties.MinioConfig("http://localhost:9000", "key", "secret", "bucket"),
                new AppProperties.EmailConfig("test@test.com"),
                new AppProperties.CorsConfig("http://localhost:5173"),
                "http://localhost:8000"
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
                    1L, "Cliente", LocalDate.now().plusDays(30), null, List.of(linea), null);

            ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
            Factura fake = facturaStub("FAC-2026-0001", new BigDecimal("500.00"),
                    new BigDecimal("65.00"), new BigDecimal("565.00"));
            when(facturaRepository.save(captor.capture())).thenReturn(fake);

            // Act
            facturaService.crear(request, "user");

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
                    1L, "Cliente", LocalDate.now().plusDays(30), null, List.of(linea), null);

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
                    1L, "C", LocalDate.now().plusDays(10), null, List.of(linea), null);
            when(facturaRepository.save(any())).thenReturn(
                    facturaStub("FAC-2026-0003", BigDecimal.TEN, BigDecimal.ONE,
                            new BigDecimal("11.00")));

            // Act
            facturaService.crear(request, "user");

            // Assert
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("debe persistir cotizacionId en la factura cuando se provee")
        void debeVincularCotizacionIdSiSeProvee() {
            // Arrange
            when(numeroGenerator.siguiente("FAC")).thenReturn("FAC-2026-0004");
            LineaFacturaRequest linea = new LineaFacturaRequest(
                    "Consultoría", new BigDecimal("1"), new BigDecimal("200.00"));
            FacturaRequest request = new FacturaRequest(
                    1L, "Cliente", LocalDate.now().plusDays(30), null, List.of(linea), 7L);

            ArgumentCaptor<Factura> captor = ArgumentCaptor.forClass(Factura.class);
            when(facturaRepository.save(captor.capture())).thenReturn(
                    facturaStub("FAC-2026-0004", new BigDecimal("200.00"),
                            new BigDecimal("26.00"), new BigDecimal("226.00")));

            // Act
            facturaService.crear(request, "user");

            // Assert
            assertThat(captor.getValue().getCotizacionId()).isEqualTo(7L);
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

    @Nested
    @DisplayName("anular()")
    class Anular {

        @Test
        @DisplayName("debe transicionar de PENDIENTE a ANULADA y registrar audit con motivo")
        void debeAnularFacturaPendiente() {
            Factura factura = facturaStub("FAC-2026-0001",
                    new BigDecimal("100.00"), new BigDecimal("13.00"), new BigDecimal("113.00"));
            when(facturaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(factura));
            when(facturaRepository.save(any())).thenReturn(factura);

            FacturaResponse response = facturaService.anular(1L, "Error de cobro", "admin");

            assertThat(response.estado()).isEqualTo(EstadoFactura.ANULADA);
            verify(auditLogRepository).save(any());
        }

        @Test
        @DisplayName("debe lanzar TransicionInvalidaException si ya está PAGADA")
        void debeLanzarExcepcionSiYaPagada() {
            Factura factura = facturaStubConEstado(EstadoFactura.PAGADA);
            when(facturaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(factura));

            assertThatThrownBy(() -> facturaService.anular(1L, "intento de anulación", "admin"))
                    .isInstanceOf(TransicionInvalidaException.class)
                    .hasMessageContaining("PAGADA");
        }

        @Test
        @DisplayName("debe lanzar TransicionInvalidaException si ya está ANULADA")
        void debeLanzarExcepcionSiYaAnulada() {
            Factura factura = facturaStubConEstado(EstadoFactura.ANULADA);
            when(facturaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(factura));

            assertThatThrownBy(() -> facturaService.anular(1L, "intento duplicado", "admin"))
                    .isInstanceOf(TransicionInvalidaException.class)
                    .hasMessageContaining("ANULADA");
        }

        @Test
        @DisplayName("debe lanzar RecursoNoEncontradoException si no existe")
        void debeLanzarExcepcionSiNoExiste() {
            when(facturaRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> facturaService.anular(99L, "motivo válido aquí", "admin"))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("AnularFacturaRequest — record")
    class AnularFacturaRequestTests {

        @Test
        @DisplayName("constructor y accessor exponen el motivo correctamente")
        void debeExponerMotivo() {
            AnularFacturaRequest req = new AnularFacturaRequest("motivo de anulación válido");
            assertThat(req.motivo()).isEqualTo("motivo de anulación válido");
        }

        @Test
        @DisplayName("dos instancias con el mismo motivo son iguales (equals/hashCode/toString de record)")
        void dosInstanciasConMismoMotivoSonIguales() {
            AnularFacturaRequest a = new AnularFacturaRequest("mismo motivo exacto aquí");
            AnularFacturaRequest b = new AnularFacturaRequest("mismo motivo exacto aquí");
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
            assertThat(a.toString()).contains("mismo motivo exacto aquí");
        }
    }

    @Nested
    @DisplayName("actualizarPdfUrl()")
    class ActualizarPdfUrl {

        @Test
        @DisplayName("debe asignar la URL del PDF y persistir la factura")
        void debeActualizarPdfUrlYPersistir() {
            Factura factura = facturaStub("FAC-2026-0001",
                    new BigDecimal("100.00"), new BigDecimal("13.00"), new BigDecimal("113.00"));
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
            when(facturaRepository.save(any())).thenReturn(factura);

            facturaService.actualizarPdfUrl(1L, "https://minio/bucket/fac.pdf");

            assertThat(factura.getPdfUrl()).isEqualTo("https://minio/bucket/fac.pdf");
            verify(facturaRepository).save(factura);
        }

        @Test
        @DisplayName("debe lanzar RecursoNoEncontradoException si no existe")
        void debeLanzarExcepcionSiNoExiste() {
            when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> facturaService.actualizarPdfUrl(99L, "url"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("descargarPdf()")
    class DescargarPdf {

        @Test
        @DisplayName("debe retornar metadata y registrar audit cuando el PDF está listo")
        void debeRetornarMetadataYAudit() {
            Factura factura = facturaStub("FAC-2026-0001",
                    new BigDecimal("100.00"), new BigDecimal("13.00"), new BigDecimal("113.00"));
            factura.setPdfUrl("http://localhost:9000/bucket/facturas/FAC-2026-0001.pdf");
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

            PdfDescarga result = facturaService.descargarPdf(1L, "admin");

            assertThat(result.objectName()).isEqualTo("facturas/FAC-2026-0001.pdf");
            assertThat(result.nombreArchivo()).startsWith("Factura_FAC-2026-0001_");
            verify(auditLogRepository).save(any());
        }

        @Test
        @DisplayName("debe lanzar RecursoNoEncontradoException cuando pdfUrl es null")
        void debeLanzarExcepcionSiPdfUrlNulo() {
            Factura factura = facturaStub("FAC-2026-0001",
                    new BigDecimal("100.00"), new BigDecimal("13.00"), new BigDecimal("113.00"));
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

            assertThatThrownBy(() -> facturaService.descargarPdf(1L, "admin"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("FAC-2026-0001");
        }

        @Test
        @DisplayName("debe lanzar RecursoNoEncontradoException cuando pdfUrl está en blanco")
        void debeLanzarExcepcionSiPdfUrlEnBlanco() {
            Factura factura = facturaStub("FAC-2026-0001",
                    new BigDecimal("100.00"), new BigDecimal("13.00"), new BigDecimal("113.00"));
            factura.setPdfUrl("   ");
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

            assertThatThrownBy(() -> facturaService.descargarPdf(1L, "admin"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("FAC-2026-0001");
        }

        @Test
        @DisplayName("debe lanzar RecursoNoEncontradoException cuando factura no existe")
        void debeLanzarExcepcionSiFacturaNoExiste() {
            when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> facturaService.descargarPdf(99L, "admin"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("PdfDescarga — record")
    class PdfDescargaTests {

        @Test
        @DisplayName("constructor y accessors exponen objectName y nombreArchivo")
        void debeExponerCampos() {
            PdfDescarga pdf = new PdfDescarga("facturas/FAC-2026-0001.pdf", "Factura_FAC-2026-0001_Cliente.pdf");
            assertThat(pdf.objectName()).isEqualTo("facturas/FAC-2026-0001.pdf");
            assertThat(pdf.nombreArchivo()).isEqualTo("Factura_FAC-2026-0001_Cliente.pdf");
        }

        @Test
        @DisplayName("equals, hashCode y toString del record")
        void debeImplementarEqualsHashCodeToString() {
            PdfDescarga a = new PdfDescarga("facturas/FAC.pdf", "nombre.pdf");
            PdfDescarga b = new PdfDescarga("facturas/FAC.pdf", "nombre.pdf");
            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
            assertThat(a.toString()).contains("nombre.pdf");
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

    private Factura facturaStubConEstado(EstadoFactura estado) {
        return Factura.builder()
                .id(1L).numero("FAC-2026-0001")
                .clienteId(1L).clienteNombre("Cliente")
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .estado(estado)
                .subtotal(new BigDecimal("100.00"))
                .impuesto(new BigDecimal("13.00"))
                .total(new BigDecimal("113.00"))
                .saldo(BigDecimal.ZERO)
                .creadoEn(LocalDateTime.now()).creadoPor("user")
                .lineas(List.of()).build();
    }
}
