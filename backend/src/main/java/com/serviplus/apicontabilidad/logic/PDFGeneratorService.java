package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.domain.Factura;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <<Template Method Pattern>> — Defines the skeleton for generating a PDF
 * document (setup → header → line items → totals → upload).
 * Each step is a protected method overrideable by subclasses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PDFGeneratorService {

    private final MinioClient minioClient;
    private final AppProperties appProperties;

    public String generarYSubirFactura(Factura factura) {
        try {
            byte[] pdfBytes = generarPDF(factura);
            return subirAMinio(factura.getNumero(), pdfBytes);
        } catch (Exception e) {
            log.error("Error al generar PDF para factura {}: {}", factura.getNumero(), e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF de la factura", e);
        }
    }

    protected byte[] generarPDF(Factura factura) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                escribirEncabezado(content, fontBold, factura);
                escribirDatosCliente(content, fontRegular, factura);
                escribirTotales(content, fontBold, fontRegular, factura);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    protected void escribirEncabezado(PDPageContentStream content, PDType1Font font, Factura factura)
            throws IOException {
        content.beginText();
        content.setFont(font, 18);
        content.newLineAtOffset(50, 780);
        content.showText("FACTURA " + factura.getNumero());
        content.endText();

        content.beginText();
        content.setFont(font, 10);
        content.newLineAtOffset(50, 760);
        content.showText("Estado: " + factura.getEstado().name());
        content.endText();
    }

    protected void escribirDatosCliente(PDPageContentStream content, PDType1Font font, Factura factura)
            throws IOException {
        content.beginText();
        content.setFont(font, 11);
        content.newLineAtOffset(50, 730);
        content.showText("Cliente: " + factura.getClienteNombre());
        content.newLineAtOffset(0, -15);
        content.showText("Vencimiento: " + factura.getFechaVencimiento());
        content.endText();
    }

    protected void escribirTotales(PDPageContentStream content, PDType1Font fontBold, PDType1Font fontRegular,
                                   Factura factura) throws IOException {
        content.beginText();
        content.setFont(fontRegular, 11);
        content.newLineAtOffset(50, 690);
        content.showText("Subtotal:  " + factura.getSubtotal());
        content.newLineAtOffset(0, -15);
        content.showText("Impuesto: " + factura.getImpuesto());
        content.endText();

        content.beginText();
        content.setFont(fontBold, 13);
        content.newLineAtOffset(50, 655);
        content.showText("TOTAL: " + factura.getTotal());
        content.endText();
    }

    private String subirAMinio(String numero, byte[] pdfBytes) throws IOException {
        String bucket = appProperties.minio().bucket();
        String objectName = "facturas/%s.pdf".formatted(numero);

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                            .contentType("application/pdf")
                            .build()
            );
        } catch (Exception e) {
            throw new IOException("Error al subir PDF a MinIO: " + objectName, e);
        }

        String pdfUrl = "%s/%s/%s".formatted(
                appProperties.minio().endpoint(),
                bucket,
                objectName
        );
        log.info("PDF subido a MinIO: {}", pdfUrl);
        return pdfUrl;
    }
}
