package com.serviplus.apicontabilidad.serializer.cotizacion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LineaCotizacionRequest(

        @NotBlank(message = "La descripción es requerida")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @NotNull(message = "La cantidad es requerida")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero")
        BigDecimal cantidad,

        @NotNull(message = "El precio unitario es requerido")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a cero")
        BigDecimal precioUnitario
) {
}
