package com.serviplus.apicontabilidad.serializer.cotizacion;

import com.serviplus.apicontabilidad.domain.EstadoCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CotizacionResponse(
        Long id,
        String numero,
        Long clienteId,
        String clienteNombre,
        LocalDate fechaVigencia,
        String notas,
        EstadoCotizacion estado,
        BigDecimal subtotal,
        BigDecimal impuesto,
        BigDecimal total,
        List<LineaCotizacionResponse> lineas,
        LocalDateTime creadoEn,
        String creadoPor
) {
}
