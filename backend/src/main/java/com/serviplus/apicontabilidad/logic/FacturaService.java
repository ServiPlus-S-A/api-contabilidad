package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.async.event.FacturaCreadaEvent;
import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.data.AuditLogRepository;
import com.serviplus.apicontabilidad.data.FacturaRepository;
import com.serviplus.apicontabilidad.domain.*;
import com.serviplus.apicontabilidad.serializer.factura.*;
import com.serviplus.apicontabilidad.utility.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NumeroGenerator numeroGenerator;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public FacturaResponse obtener(Long id) {
        return facturaRepository.findById(id)
                .map(FacturaSerializer::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + id));
    }

    public FacturaResponse crear(FacturaRequest request, String usuario) {
        BigDecimal ivaRate = appProperties.iva().rate();
        String numero = numeroGenerator.siguiente("FAC");

        List<LineaFactura> lineas = buildLineas(request.lineas());
        BigDecimal subtotal = sumarSubtotales(lineas);
        BigDecimal impuesto = subtotal.multiply(ivaRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(impuesto);

        Factura factura = Factura.builder()
                .numero(numero)
                .clienteId(request.clienteId())
                .clienteNombre(request.clienteNombre())
                .fechaVencimiento(request.fechaVencimiento())
                .notas(request.notas())
                .estado(EstadoFactura.PENDIENTE)
                .subtotal(subtotal)
                .impuesto(impuesto)
                .total(total)
                .saldo(total)
                .creadoEn(LocalDateTime.now(ZoneId.systemDefault()))
                .creadoPor(usuario)
                .build();

        lineas.forEach(l -> l.setFactura(factura));
        factura.setLineas(lineas);

        Factura saved = facturaRepository.save(factura);
        log.info("Factura creada: {} por {}", numero, usuario);
        registrarAudit(saved.getId(), "FACTURA", "CREAR", usuario, "Número: " + numero);

        eventPublisher.publishEvent(new FacturaCreadaEvent(this, saved));

        return FacturaSerializer.toResponse(saved);
    }

    public FacturaResponse anular(Long id, String motivo, String usuario) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + id));

        if (!factura.getEstado().puedeTransicionarA(EstadoFactura.ANULADA)) {
            throw new TransicionInvalidaException(
                    "Transición inválida: %s → ANULADA".formatted(factura.getEstado()));
        }

        factura.setEstado(EstadoFactura.ANULADA);
        factura.setActualizadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        facturaRepository.save(factura);
        registrarAudit(id, "FACTURA", "ANULAR", usuario, "Motivo: " + motivo);
        log.info("Factura {} anulada por {}", factura.getNumero(), usuario);

        return FacturaSerializer.toResponse(factura);
    }

    public void actualizarPdfUrl(Long id, String pdfUrl) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + id));
        factura.setPdfUrl(pdfUrl);
        factura.setActualizadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        facturaRepository.save(factura);
    }

    private List<LineaFactura> buildLineas(List<LineaFacturaRequest> requests) {
        return requests.stream().map(r -> {
            BigDecimal subtotalLinea = r.precioUnitario()
                    .multiply(r.cantidad())
                    .setScale(2, RoundingMode.HALF_UP);
            return LineaFactura.builder()
                    .descripcion(r.descripcion())
                    .cantidad(r.cantidad())
                    .precioUnitario(r.precioUnitario())
                    .subtotal(subtotalLinea)
                    .build();
        }).toList();
    }

    private BigDecimal sumarSubtotales(List<LineaFactura> lineas) {
        return lineas.stream()
                .map(LineaFactura::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void registrarAudit(Long entidadId, String entidad, String accion, String usuario, String detalle) {
        auditLogRepository.save(AuditLog.builder()
                .entidad(entidad)
                .entidadId(entidadId)
                .accion(accion)
                .usuario(usuario)
                .detalle(detalle)
                .timestamp(LocalDateTime.now(ZoneId.systemDefault()))
                .build());
    }
}
