package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.data.AbonoRepository;
import com.serviplus.apicontabilidad.data.AuditLogRepository;
import com.serviplus.apicontabilidad.data.FacturaRepository;
import com.serviplus.apicontabilidad.domain.*;
import com.serviplus.apicontabilidad.serializer.abono.*;
import com.serviplus.apicontabilidad.utility.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <<Strategy Pattern>> — Encapsulates the business rules for registering
 * partial payments (abonos) against a Factura. Validates that:
 *  - monto > 0
 *  - monto <= saldo actual
 *  - Factura is in PENDIENTE state
 * Automatically transitions Factura to PAGADA when saldo reaches zero.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PagoService {

    private final FacturaRepository facturaRepository;
    private final AbonoRepository abonoRepository;
    private final AuditLogRepository auditLogRepository;

    public AbonoResponse registrarAbono(Long facturaId, AbonoRequest request, String usuario) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + facturaId));

        validarMontoPositivo(request.monto());
        validarFacturaPendiente(factura);
        validarMontoNoSuperaSaldo(request.monto(), factura.getSaldo());

        Abono abono = Abono.builder()
                .factura(factura)
                .monto(request.monto())
                .fecha(LocalDateTime.now())
                .referencia(request.referencia())
                .creadoPor(usuario)
                .build();

        BigDecimal nuevoSaldo = factura.getSaldo().subtract(request.monto());
        factura.setSaldo(nuevoSaldo);
        factura.setActualizadoEn(LocalDateTime.now());

        if (nuevoSaldo.compareTo(BigDecimal.ZERO) == 0) {
            factura.setEstado(EstadoFactura.PAGADA);
            log.info("Factura {} pagada completamente por {}", factura.getNumero(), usuario);
        }

        facturaRepository.save(factura);
        Abono saved = abonoRepository.save(abono);
        registrarAudit(facturaId, "ABONO", "REGISTRAR", usuario,
                "Monto: %s, Saldo restante: %s".formatted(request.monto(), nuevoSaldo));

        return AbonoSerializer.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AbonoResponse> listarPorFactura(Long facturaId) {
        if (!facturaRepository.existsById(facturaId)) {
            throw new RecursoNoEncontradoException("Factura no encontrada: " + facturaId);
        }
        return abonoRepository.findByFacturaIdOrderByFechaDesc(facturaId).stream()
                .map(AbonoSerializer::toResponse)
                .toList();
    }

    private void validarMontoPositivo(BigDecimal monto) {
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El monto del abono debe ser mayor a cero");
        }
    }

    private void validarFacturaPendiente(Factura factura) {
        if (factura.getEstado() != EstadoFactura.PENDIENTE) {
            throw new ReglaNegocioException(
                    "Solo se pueden registrar abonos en facturas PENDIENTE. Estado actual: " + factura.getEstado());
        }
    }

    private void validarMontoNoSuperaSaldo(BigDecimal monto, BigDecimal saldo) {
        if (monto.compareTo(saldo) > 0) {
            throw new ReglaNegocioException(
                    "El monto (%s) supera el saldo de la factura (%s)".formatted(monto, saldo));
        }
    }

    private void registrarAudit(Long entidadId, String entidad, String accion, String usuario, String detalle) {
        auditLogRepository.save(AuditLog.builder()
                .entidad(entidad)
                .entidadId(entidadId)
                .accion(accion)
                .usuario(usuario)
                .detalle(detalle)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
