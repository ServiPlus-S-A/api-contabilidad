package com.serviplus.apicontabilidad.serializer.cotizacion;

import java.math.BigDecimal;

public record LineaCotizacionResponse(
        Long id,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
