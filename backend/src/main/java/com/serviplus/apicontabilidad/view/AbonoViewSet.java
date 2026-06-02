package com.serviplus.apicontabilidad.view;

import com.serviplus.apicontabilidad.logic.PagoService;
import com.serviplus.apicontabilidad.serializer.abono.AbonoRequest;
import com.serviplus.apicontabilidad.serializer.abono.AbonoResponse;
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
 * <<Facade Pattern>> — Exposes Abono (partial payment) use cases as a REST API.
 */
@RestController
@RequestMapping("/api/v1/facturas/{facturaId}/abonos")
@RequiredArgsConstructor
@Tag(name = "Abonos", description = "Registro de pagos parciales sobre facturas")
public class AbonoViewSet {

    private final PagoService pagoService;

    @GetMapping
    @Operation(summary = "Lista los abonos registrados para una factura")
    public ResponseEntity<List<AbonoResponse>> listar(@PathVariable Long facturaId) {
        return ResponseEntity.ok(pagoService.listarPorFactura(facturaId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @Operation(summary = "Registra un abono — reduce el saldo; marca PAGADA si saldo = 0")
    public ResponseEntity<AbonoResponse> registrar(
            @PathVariable Long facturaId,
            @Valid @RequestBody AbonoRequest request,
            Authentication auth) {
        AbonoResponse response = pagoService.registrarAbono(facturaId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
