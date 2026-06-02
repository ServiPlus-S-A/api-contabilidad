package com.serviplus.apicontabilidad.serializer.factura;

import com.serviplus.apicontabilidad.domain.EstadoFactura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaResponse(
        Long id,
        String numero,
        Long clienteId,
        String clienteNombre,
        LocalDate fechaVencimiento,
        String notas,
        EstadoFactura estado,
        BigDecimal subtotal,
        BigDecimal impuesto,
        BigDecimal total,
        BigDecimal saldo,
        String pdfUrl,
        List<LineaFacturaResponse> lineas,
        LocalDateTime creadoEn,
        String creadoPor
) {
}
