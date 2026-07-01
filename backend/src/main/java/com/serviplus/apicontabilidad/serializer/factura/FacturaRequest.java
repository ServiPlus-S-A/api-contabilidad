package com.serviplus.apicontabilidad.serializer.factura;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record FacturaRequest(

        @NotNull(message = "El ID de cliente es requerido")
        @Min(value = 1, message = "El ID de cliente debe ser mayor a cero")
        Long clienteId,

        @NotBlank(message = "El nombre del cliente es requerido")
        @Size(max = 255, message = "El nombre del cliente no puede superar los 255 caracteres")
        String clienteNombre,

        @NotNull(message = "La fecha de vencimiento es requerida")
        @Future(message = "La fecha de vencimiento debe ser futura")
        LocalDate fechaVencimiento,

        @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres")
        String notas,

        @NotNull(message = "Debe incluir al menos una línea")
        @Size(min = 1, message = "Debe incluir al menos una línea de detalle")
        @Valid
        List<LineaFacturaRequest> lineas,

        @Min(value = 1, message = "El ID de cotización debe ser mayor a cero")
        Long cotizacionId
) {
}
