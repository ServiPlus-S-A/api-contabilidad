package com.serviplus.apicontabilidad.view;

import com.serviplus.apicontabilidad.logic.FacturaService;
import com.serviplus.apicontabilidad.logic.PDFGeneratorService;
import com.serviplus.apicontabilidad.serializer.factura.AnularFacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.FacturaRequest;
import com.serviplus.apicontabilidad.serializer.factura.FacturaResponse;
import com.serviplus.apicontabilidad.serializer.factura.PdfDescarga;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * <<Facade Pattern>> — Exposes Factura use cases as a REST API.
 */
@RestController
@RequestMapping("/api/v1/facturas")
@RequiredArgsConstructor
@Tag(name = "Facturas", description = "Creación y consulta de facturas")
public class FacturaViewSet {

    private final FacturaService facturaService;
    private final PDFGeneratorService pdfGeneratorService;

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

    @PutMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMIN','CONTADOR')")
    @Operation(summary = "Anula una factura PENDIENTE — registra motivo en audit log")
    public ResponseEntity<FacturaResponse> anular(
            @PathVariable Long id,
            @Valid @RequestBody AnularFacturaRequest request,
            Authentication auth) {
        return ResponseEntity.ok(facturaService.anular(id, request.motivo(), auth.getName()));
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Descarga el PDF de una factura desde MinIO")
    public ResponseEntity<StreamingResponseBody> descargarPdf(
            @PathVariable Long id, Authentication auth) {
        PdfDescarga pdf = facturaService.descargarPdf(id, auth.getName());
        StreamingResponseBody body = out -> pdfGeneratorService.streamearDeMinio(pdf.objectName(), out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + pdf.nombreArchivo() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(body);
    }
}
