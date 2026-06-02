package com.serviplus.apicontabilidad.serializer.cotizacion;

import com.serviplus.apicontabilidad.domain.Cotizacion;
import com.serviplus.apicontabilidad.domain.LineaCotizacion;

import java.util.List;

/**
 * <<DTO/Adapter Pattern>> — Maps between Cotizacion domain entities and DTOs.
 * Static utility class: no state, no Spring bean needed.
 */
public final class CotizacionSerializer {

    private CotizacionSerializer() {
    }

    public static CotizacionResponse toResponse(Cotizacion cotizacion) {
        return new CotizacionResponse(
                cotizacion.getId(),
                cotizacion.getNumero(),
                cotizacion.getClienteId(),
                cotizacion.getClienteNombre(),
                cotizacion.getFechaVigencia(),
                cotizacion.getNotas(),
                cotizacion.getEstado(),
                cotizacion.getSubtotal(),
                cotizacion.getImpuesto(),
                cotizacion.getTotal(),
                toLineaResponses(cotizacion.getLineas()),
                cotizacion.getCreadoEn(),
                cotizacion.getCreadoPor()
        );
    }

    private static List<LineaCotizacionResponse> toLineaResponses(List<LineaCotizacion> lineas) {
        return lineas.stream()
                .map(l -> new LineaCotizacionResponse(
                        l.getId(),
                        l.getDescripcion(),
                        l.getCantidad(),
                        l.getPrecioUnitario(),
                        l.getSubtotal()
                ))
                .toList();
    }
}
