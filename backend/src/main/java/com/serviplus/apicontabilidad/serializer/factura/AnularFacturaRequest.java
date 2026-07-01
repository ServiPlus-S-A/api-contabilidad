package com.serviplus.apicontabilidad.serializer.factura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularFacturaRequest(

        @NotBlank(message = "El motivo de anulación es requerido")
        @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
        String motivo
) {
}
