package com.serviplus.apicontabilidad.serializer.factura;

import com.serviplus.apicontabilidad.domain.Factura;
import com.serviplus.apicontabilidad.domain.LineaFactura;

import java.util.List;

/**
 * <<DTO/Adapter Pattern>> — Maps between Factura domain entities and DTOs.
 */
public final class FacturaSerializer {

    private FacturaSerializer() {
    }

    public static FacturaResponse toResponse(Factura factura) {
        return new FacturaResponse(
                factura.getId(),
                factura.getNumero(),
                factura.getClienteId(),
                factura.getClienteNombre(),
                factura.getFechaVencimiento(),
                factura.getNotas(),
                factura.getEstado(),
                factura.getSubtotal(),
                factura.getImpuesto(),
                factura.getTotal(),
                factura.getSaldo(),
                factura.getPdfUrl(),
                toLineaResponses(factura.getLineas()),
                factura.getCreadoEn(),
                factura.getCreadoPor()
        );
    }

    private static List<LineaFacturaResponse> toLineaResponses(List<LineaFactura> lineas) {
        return lineas.stream()
                .map(l -> new LineaFacturaResponse(
                        l.getId(),
                        l.getDescripcion(),
                        l.getCantidad(),
                        l.getPrecioUnitario(),
                        l.getSubtotal()
                ))
                .toList();
    }
}
