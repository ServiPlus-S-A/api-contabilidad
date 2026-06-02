package com.serviplus.apicontabilidad.serializer.abono;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AbonoRequest(

        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @Size(max = 255, message = "La referencia no puede superar los 255 caracteres")
        String referencia
) {
}
