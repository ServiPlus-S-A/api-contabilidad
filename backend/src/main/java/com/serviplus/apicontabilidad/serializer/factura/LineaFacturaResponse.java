package com.serviplus.apicontabilidad.serializer.factura;

import java.math.BigDecimal;

public record LineaFacturaResponse(
        Long id,
        String descripcion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
