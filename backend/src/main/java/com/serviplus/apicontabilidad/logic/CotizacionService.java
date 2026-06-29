package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.async.event.CotizacionAprobadaEvent;
import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.data.AuditLogRepository;
import com.serviplus.apicontabilidad.data.CotizacionRepository;
import com.serviplus.apicontabilidad.domain.*;
import com.serviplus.apicontabilidad.serializer.cotizacion.*;
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
import java.util.Collection;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CotizacionService {

    private final CotizacionRepository cotizacionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NumeroGenerator numeroGenerator;
    private final AppProperties appProperties;

    private static final String ENTIDAD_COT = "COTIZACION";

    @Transactional(readOnly = true)
    public List<CotizacionResponse> listar(String usuario, Collection<String> roles) {
        boolean esAdminOContador = roles.stream()
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_CONTADOR"));

        List<Cotizacion> cotizaciones = esAdminOContador
                ? cotizacionRepository.findAllByOrderByCreadoEnDesc()
                : cotizacionRepository.findByCreadoPorOrderByCreadoEnDesc(usuario);

        return cotizaciones.stream()
                .map(CotizacionSerializer::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CotizacionResponse obtener(Long id) {
        return cotizacionRepository.findById(id)
                .map(CotizacionSerializer::toResponse)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotización no encontrada: " + id));
    }

    public CotizacionResponse crear(CotizacionRequest request, String usuario) {
        BigDecimal ivaRate = appProperties.iva().rate();
        String numero = numeroGenerator.siguiente("COT");

        List<LineaCotizacion> lineas = buildLineas(request.lineas());
        BigDecimal subtotal = sumarSubtotales(lineas);
        BigDecimal impuesto = subtotal.multiply(ivaRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(impuesto);

        Cotizacion cotizacion = Cotizacion.builder()
                .numero(numero)
                .clienteId(request.clienteId())
                .clienteNombre(request.clienteNombre())
                .fechaVigencia(request.fechaVigencia())
                .notas(request.notas())
                .estado(EstadoCotizacion.BORRADOR)
                .subtotal(subtotal)
                .impuesto(impuesto)
                .total(total)
                .creadoEn(LocalDateTime.now(ZoneId.systemDefault()))
                .creadoPor(usuario)
                .build();

        lineas.forEach(l -> l.setCotizacion(cotizacion));
        cotizacion.setLineas(lineas);

        Cotizacion saved = cotizacionRepository.save(cotizacion);
        log.info("Cotización creada: {} por {}", numero, usuario);
        registrarAudit(saved.getId(), ENTIDAD_COT, "CREAR", usuario, "Número: " + numero);

        return CotizacionSerializer.toResponse(saved);
    }

    public CotizacionResponse aprobar(Long id, String usuario) {
        Cotizacion cotizacion = buscarOFallar(id);
        transicionar(cotizacion, EstadoCotizacion.ACEPTADA);
        cotizacion.setActualizadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        cotizacionRepository.save(cotizacion);
        registrarAudit(id, ENTIDAD_COT, "APROBAR", usuario, cotizacion.getNumero());

        eventPublisher.publishEvent(new CotizacionAprobadaEvent(this, cotizacion));
        log.info("Cotización {} aprobada por {}", cotizacion.getNumero(), usuario);

        return CotizacionSerializer.toResponse(cotizacion);
    }

    public CotizacionResponse rechazar(Long id, String usuario) {
        Cotizacion cotizacion = buscarOFallar(id);
        transicionar(cotizacion, EstadoCotizacion.RECHAZADA);
        cotizacion.setActualizadoEn(LocalDateTime.now(ZoneId.systemDefault()));
        cotizacionRepository.save(cotizacion);
        registrarAudit(id, ENTIDAD_COT, "RECHAZAR", usuario, cotizacion.getNumero());
        log.info("Cotización {} rechazada por {}", cotizacion.getNumero(), usuario);

        return CotizacionSerializer.toResponse(cotizacion);
    }

    private Cotizacion buscarOFallar(Long id) {
        return cotizacionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotización no encontrada: " + id));
    }

    private void transicionar(Cotizacion cotizacion, EstadoCotizacion destino) {
        if (!cotizacion.getEstado().puedeTransicionarA(destino)) {
            throw new TransicionInvalidaException(
                    "Transición inválida: %s → %s".formatted(cotizacion.getEstado(), destino));
        }
        cotizacion.setEstado(destino);
    }

    private List<LineaCotizacion> buildLineas(List<LineaCotizacionRequest> requests) {
        return requests.stream().map(r -> {
            BigDecimal subtotalLinea = r.precioUnitario()
                    .multiply(r.cantidad())
                    .setScale(2, RoundingMode.HALF_UP);
            return LineaCotizacion.builder()
                    .descripcion(r.descripcion())
                    .cantidad(r.cantidad())
                    .precioUnitario(r.precioUnitario())
                    .subtotal(subtotalLinea)
                    .build();
        }).toList();
    }

    private BigDecimal sumarSubtotales(List<LineaCotizacion> lineas) {
        return lineas.stream()
                .map(LineaCotizacion::getSubtotal)
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
