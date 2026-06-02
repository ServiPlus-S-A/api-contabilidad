package com.serviplus.apicontabilidad.view;

import com.serviplus.apicontabilidad.logic.FacturaService;
import com.serviplus.apicontabilidad.serializer.factura.FacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.FacturaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * <<Facade Pattern>> — Exposes Factura use cases as a REST API.
 */
@RestController
@RequestMapping("/api/v1/facturas")
@RequiredArgsConstructor
@Tag(name = "Facturas", description = "Creación y consulta de facturas")
public class FacturaViewSet {

    private final FacturaService facturaService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene el detalle completo de una factura con su estado y PDF URL")
    public ResponseEntity<FacturaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva factura — dispara generación de PDF async")
    public ResponseEntity<FacturaResponse> crear(
            @Valid @RequestBody FacturaRequest request,
            Authentication auth) {
        FacturaResponse response = facturaService.crear(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
