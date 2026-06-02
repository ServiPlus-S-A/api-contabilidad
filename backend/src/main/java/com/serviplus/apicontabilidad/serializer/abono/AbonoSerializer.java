package com.serviplus.apicontabilidad.serializer.abono;

import com.serviplus.apicontabilidad.domain.Abono;

/**
 * <<DTO/Adapter Pattern>> — Maps between Abono domain entities and DTOs.
 */
public final class AbonoSerializer {

    private AbonoSerializer() {
    }

    public static AbonoResponse toResponse(Abono abono) {
        return new AbonoResponse(
                abono.getId(),
                abono.getFactura().getId(),
                abono.getMonto(),
                abono.getFecha(),
                abono.getReferencia(),
                abono.getCreadoPor()
        );
    }
}
