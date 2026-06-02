package com.serviplus.apicontabilidad.async;

import com.serviplus.apicontabilidad.async.event.FacturaCreadaEvent;
import com.serviplus.apicontabilidad.domain.Factura;
import com.serviplus.apicontabilidad.logic.FacturaService;
import com.serviplus.apicontabilidad.logic.PDFGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <<Command Pattern>> — Listens for FacturaCreadaEvent and asynchronously
 * generates a PDF, uploading it to MinIO, then stores the public URL on the Factura.
 * Strict layering preserved: updates Factura via FacturaService, not the repository.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PDFGeneratorTask {

    private final PDFGeneratorService pdfGeneratorService;
    private final FacturaService facturaService;

    @Async("taskExecutor")
    @EventListener
    public void onFacturaCreada(FacturaCreadaEvent event) {
        Factura factura = event.getFactura();
        try {
            String pdfUrl = pdfGeneratorService.generarYSubirFactura(factura);
            facturaService.actualizarPdfUrl(factura.getId(), pdfUrl);
            log.info("PDF generado y enlace persistido para factura {}", factura.getNumero());
        } catch (Exception e) {
            log.error("Error al generar PDF para factura {}: {}", factura.getNumero(), e.getMessage());
        }
    }
}
