package com.serviplus.apicontabilidad.serializer.abono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AbonoResponse(
        Long id,
        Long facturaId,
        BigDecimal monto,
        LocalDateTime fecha,
        String referencia,
        String creadoPor
) {
}
