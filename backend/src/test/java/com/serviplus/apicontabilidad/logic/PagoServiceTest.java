package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.data.AbonoRepository;
import com.serviplus.apicontabilidad.data.AuditLogRepository;
import com.serviplus.apicontabilidad.data.FacturaRepository;
import com.serviplus.apicontabilidad.domain.Abono;
import com.serviplus.apicontabilidad.domain.EstadoFactura;
import com.serviplus.apicontabilidad.domain.Factura;
import com.serviplus.apicontabilidad.serializer.abono.AbonoRequest;
import com.serviplus.apicontabilidad.serializer.abono.AbonoResponse;
import com.serviplus.apicontabilidad.utility.ReglaNegocioException;
import com.serviplus.apicontabilidad.utility.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoService — unit tests")
class PagoServiceTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private AbonoRepository abonoRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private PagoService pagoService;

    @BeforeEach
    void setUp() {
        pagoService = new PagoService(facturaRepository, abonoRepository, auditLogRepository);
    }

    @Nested
    @DisplayName("registrarAbono()")
    class RegistrarAbono {

        @Test
        @DisplayName("debe reducir el saldo de la factura por el monto del abono")
        void debeReducirElSaldo() {
            // Arrange
            Factura factura = facturaConSaldo(new BigDecimal("500.00"));
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
            Abono savedAbono = abonoStub(factura, new BigDecimal("200.00"));
            when(abonoRepository.save(any())).thenReturn(savedAbono);

            AbonoRequest request = new AbonoRequest(new BigDecimal("200.00"), "REF-001");

            // Act
            pagoService.registrarAbono(1L, request, "cajero");

            // Assert
            assertThat(factura.getSaldo()).isEqualByComparingTo("300.00");
            assertThat(factura.getEstado()).isEqualTo(EstadoFactura.PENDIENTE);
        }

        @Test
        @DisplayName("debe marcar la factura PAGADA cuando el saldo llega a cero")
        void debePagarFacturaCuandoSaldoCero() {
            // Arrange
            Factura factura = facturaConSaldo(new BigDecimal("300.00"));
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
            when(abonoRepository.save(any())).thenReturn(abonoStub(factura, new BigDecimal("300.00")));

            AbonoRequest request = new AbonoRequest(new BigDecimal("300.00"), null);

            // Act
            pagoService.registrarAbono(1L, request, "cajero");

            // Assert
            assertThat(factura.getSaldo()).isEqualByComparingTo("0.00");
            assertThat(factura.getEstado()).isEqualTo(EstadoFactura.PAGADA);
        }

        @Test
        @DisplayName("debe lanzar ReglaNegocioException si el monto supera el saldo")
        void debeLanzarExcepcionSiMontoSuperaSaldo() {
            // Arrange
            Factura factura = facturaConSaldo(new BigDecimal("100.00"));
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

            AbonoRequest request = new AbonoRequest(new BigDecimal("150.00"), null);

            // Act & Assert
            assertThatThrownBy(() -> pagoService.registrarAbono(1L, request, "cajero"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("saldo");
        }

        @Test
        @DisplayName("debe lanzar ReglaNegocioException si el monto es cero o negativo")
        void debeLanzarExcepcionSiMontoCeroONegativo() {
            // Arrange
            Factura factura = facturaConSaldo(new BigDecimal("100.00"));
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

            AbonoRequest requestCero = new AbonoRequest(BigDecimal.ZERO, null);

            // Act & Assert
            assertThatThrownBy(() -> pagoService.registrarAbono(1L, requestCero, "cajero"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("mayor a cero");
        }

        @Test
        @DisplayName("debe lanzar ReglaNegocioException si la factura no está PENDIENTE")
        void debeLanzarExcepcionSiFacturaNoPendiente() {
            // Arrange
            Factura factura = facturaConSaldo(new BigDecimal("0.00"));
            factura.setEstado(EstadoFactura.PAGADA);
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

            AbonoRequest request = new AbonoRequest(new BigDecimal("50.00"), null);

            // Act & Assert
            assertThatThrownBy(() -> pagoService.registrarAbono(1L, request, "cajero"))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("PENDIENTE");
        }

        @Test
        @DisplayName("debe lanzar RecursoNoEncontradoException si la factura no existe")
        void debeLanzarExcepcionSiFacturaNoExiste() {
            // Arrange
            when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

            AbonoRequest request = new AbonoRequest(new BigDecimal("100.00"), null);

            // Act & Assert
            assertThatThrownBy(() -> pagoService.registrarAbono(99L, request, "cajero"))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("debe registrar entrada en audit_log con monto y saldo restante")
        void debeRegistrarAuditLog() {
            // Arrange
            Factura factura = facturaConSaldo(new BigDecimal("500.00"));
            when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
            when(abonoRepository.save(any())).thenReturn(abonoStub(factura, new BigDecimal("100.00")));

            AbonoRequest request = new AbonoRequest(new BigDecimal("100.00"), "REF-X");

            // Act
            pagoService.registrarAbono(1L, request, "cajero");

            // Assert
            verify(auditLogRepository).save(argThat(log ->
                    log.getAccion().equals("REGISTRAR") && log.getEntidad().equals("ABONO")));
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Factura facturaConSaldo(BigDecimal saldo) {
        return Factura.builder()
                .id(1L).numero("FAC-2026-0001")
                .clienteId(1L).clienteNombre("Test")
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .estado(EstadoFactura.PENDIENTE)
                .subtotal(saldo).impuesto(BigDecimal.ZERO)
                .total(saldo).saldo(saldo)
                .creadoEn(LocalDateTime.now()).creadoPor("user")
                .lineas(List.of()).build();
    }

    private Abono abonoStub(Factura factura, BigDecimal monto) {
        return Abono.builder()
                .id(1L).factura(factura).monto(monto)
                .fecha(LocalDateTime.now()).creadoPor("cajero")
                .build();
    }
}
