package com.serviplus.apicontabilidad.view;

import com.serviplus.apicontabilidad.logic.CotizacionService;
import com.serviplus.apicontabilidad.serializer.cotizacion.CotizacionRequest;
import com.serviplus.apicontabilidad.serializer.cotizacion.CotizacionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <<Facade Pattern>> — Exposes Cotizacion use cases as a REST API.
 * Strict layering: delegates all logic to CotizacionService; no direct data access.
 */
@RestController
@RequestMapping("/api/v1/cotizaciones")
@RequiredArgsConstructor
@Tag(name = "Cotizaciones", description = "Gestión del ciclo de vida de cotizaciones")
public class CotizacionViewSet {

    private final CotizacionService cotizacionService;

    @GetMapping("/facturables")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @Operation(summary = "Lista cotizaciones ACEPTADAS sin factura asociada — para cargar datos de atenciones")
    public ResponseEntity<List<CotizacionResponse>> listarFacturables() {
        return ResponseEntity.ok(cotizacionService.listarFacturables());
    }

    @GetMapping
    @Operation(summary = "Lista cotizaciones — todas (admin/contador) o propias (cliente)")
    public ResponseEntity<List<CotizacionResponse>> listar(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(Object::toString)
                .toList();
        return ResponseEntity.ok(cotizacionService.listar(auth.getName(), roles));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene el detalle completo de una cotización")
    public ResponseEntity<CotizacionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(cotizacionService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea una nueva cotización en estado BORRADOR")
    public ResponseEntity<CotizacionResponse> crear(
            @Valid @RequestBody CotizacionRequest request,
            Authentication auth) {
        CotizacionResponse response = cotizacionService.crear(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @Operation(summary = "Aprueba una cotización ENVIADA — dispara email y PDF async")
    public ResponseEntity<CotizacionResponse> aprobar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(cotizacionService.aprobar(id, auth.getName()));
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @Operation(summary = "Rechaza una cotización ENVIADA")
    public ResponseEntity<CotizacionResponse> rechazar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(cotizacionService.rechazar(id, auth.getName()));
    }

    @PutMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @Operation(summary = "Marca la cotización como ENVIADA para revisión — transición BORRADOR → ENVIADA")
    public ResponseEntity<CotizacionResponse> enviar(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(cotizacionService.enviar(id, auth.getName()));
    }

    @PutMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @Operation(summary = "Anula la cotización — válido desde BORRADOR o ENVIADA")
    public ResponseEntity<CotizacionResponse> anular(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(cotizacionService.anular(id, auth.getName()));
    }
}
