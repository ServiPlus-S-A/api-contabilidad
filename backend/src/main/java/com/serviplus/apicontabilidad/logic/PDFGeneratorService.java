package com.serviplus.apicontabilidad.logic;

import com.serviplus.apicontabilidad.config.AppProperties;
import com.serviplus.apicontabilidad.domain.Factura;
import com.serviplus.apicontabilidad.domain.LineaFactura;
import com.serviplus.apicontabilidad.utility.RecursoNoEncontradoException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Generates professional A4 invoices with PDFBox and uploads them to MinIO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PDFGeneratorService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String FMT_MONEY = "$%,.2f";

    // ── Color palette ──────────────────────────────────────────────────────────
    private static final Color NAVY      = new Color(29,  52,  90);
    private static final Color BLUE      = new Color(44, 120, 172);
    private static final Color LIGHT_BG  = new Color(237, 245, 252);
    private static final Color ROW_ALT   = new Color(246, 247, 249);
    private static final Color WHITE     = Color.WHITE;
    private static final Color DARK      = new Color(42,  51,  62);
    private static final Color SECONDARY = new Color(101, 112, 126);
    private static final Color BORDER    = new Color(218, 222, 228);
    private static final Color SUBTITLE  = new Color(168, 196, 224);
    private static final Color GREEN     = new Color(27, 130,  65);
    private static final Color RED       = new Color(168,  30,  30);
    private static final Color ORANGE    = new Color(186, 106,  18);

    // ── Layout constants (A4 in points) ───────────────────────────────────────
    private static final float PAGE_W  = PDRectangle.A4.getWidth();
    private static final float PAGE_H  = PDRectangle.A4.getHeight();
    private static final float MARGIN  = 40f;
    private static final float C_W     = PAGE_W - 2 * MARGIN;

    // Table columns (sum = C_W = 515.27)
    private static final float C_NUM   = 22f;
    private static final float C_DESC  = 243f;
    private static final float C_QTY   = 60f;
    private static final float C_PRICE = 94f;

    private static final float ROW_H  = 22f;
    private static final float HEAD_H = 20f;

    private final MinioClient minioClient;
    private final AppProperties appProperties;

    // ── Public API ─────────────────────────────────────────────────────────────

    public String generarYSubirFactura(Factura factura) {
        try {
            byte[] pdf = generarPDF(factura);
            return subirAMinio(factura.getNumero(), pdf);
        } catch (Exception e) {
            log.error("Error al generar PDF para factura {}: {}", factura.getNumero(), e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF de la factura", e);
        }
    }

    public void streamearDeMinio(String objectName, OutputStream out) {
        String bucket = appProperties.minio().bucket();
        try (var stream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectName).build())) {
            stream.transferTo(out);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new RecursoNoEncontradoException("PDF no encontrado en MinIO: " + objectName);
            }
            throw new RuntimeException("Error al descargar PDF de MinIO: " + objectName, e);
        } catch (Exception e) {
            throw new RuntimeException("Error al descargar PDF de MinIO: " + objectName, e);
        }
    }

    // ── PDF generation ─────────────────────────────────────────────────────────

    protected byte[] generarPDF(Factura factura) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_H;
                y = seccionEncabezado(cs, bold, regular, factura, y);
                y = seccionBandaInfo(cs, bold, regular, factura, y);
                y = seccionCliente(cs, bold, regular, factura, y);
                y = seccionTabla(cs, bold, regular, factura, y);
                y = seccionTotales(cs, bold, regular, factura, y);
                if (factura.getNotas() != null && !factura.getNotas().isBlank()) {
                    seccionNotas(cs, bold, regular, factura, y);
                }
                seccionPie(cs, regular);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    // ── 1. Header ──────────────────────────────────────────────────────────────

    private float seccionEncabezado(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
            Factura factura, float y) throws IOException {
        float h = 64f;
        rect(cs, NAVY, 0, y - h, PAGE_W, h);

        // Left: company
        txt(cs, bold, 16, WHITE, MARGIN, y - 24, "SERVIPLUS S.A.");
        txt(cs, regular, 9, SUBTITLE, MARGIN, y - 40, "Sistema de Contabilidad");

        // Right: FACTURA label + number
        float titleW = sw(bold, 26, "FACTURA");
        txt(cs, bold, 26, WHITE, PAGE_W - MARGIN - titleW, y - 22, "FACTURA");

        float numW = sw(regular, 10, factura.getNumero());
        txt(cs, regular, 10, SUBTITLE, PAGE_W - MARGIN - numW, y - 40, factura.getNumero());

        return y - h;
    }

    // ── 2. Info band ───────────────────────────────────────────────────────────

    private float seccionBandaInfo(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
            Factura factura, float y) throws IOException {
        float h = 36f;
        rect(cs, LIGHT_BG, 0, y - h, PAGE_W, h);

        float labY = y - 10;
        float valY = y - 23;

        // Emission date
        txt(cs, bold, 7.5f, BLUE, MARGIN, labY, "FECHA DE EMISION");
        txt(cs, regular, 9.5f, DARK, MARGIN, valY, factura.getCreadoEn().format(DATE_FMT));

        // Due date
        float cx = PAGE_W / 2f - 55;
        txt(cs, bold, 7.5f, BLUE, cx, labY, "VENCIMIENTO");
        txt(cs, regular, 9.5f, DARK, cx, valY, factura.getFechaVencimiento().format(DATE_FMT));

        // Estado badge
        String estado = factura.getEstado().name();
        Color badgeColor = switch (estado) {
            case "PAGADA"  -> GREEN;
            case "ANULADA" -> RED;
            default        -> ORANGE;
        };
        float bW = 92f;
        float bH = 22f;
        float bX = PAGE_W - MARGIN - bW;
        float bY = y - h / 2f - bH / 2f;
        rect(cs, badgeColor, bX, bY, bW, bH);
        float estW = sw(bold, 9, estado);
        txt(cs, bold, 9, WHITE, bX + (bW - estW) / 2, bY + 7, estado);

        return y - h;
    }

    // ── 3. Client section ──────────────────────────────────────────────────────

    private float seccionCliente(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
            Factura factura, float y) throws IOException {
        y -= 16;
        float barH = 64f;

        // Accent left bar
        rect(cs, BLUE, MARGIN, y - barH, 3, barH);

        txt(cs, bold, 7.5f, BLUE, MARGIN + 9, y - 10, "FACTURAR A");
        txt(cs, bold, 13, DARK, MARGIN + 9, y - 27, factura.getClienteNombre());
        txt(cs, regular, 9, SECONDARY, MARGIN + 9, y - 43, "Codigo de cliente: " + factura.getClienteId());

        y -= barH + 12;
        line(cs, BORDER, 0.7f, MARGIN, y, PAGE_W - MARGIN, y);

        return y - 14;
    }

    // ── 4. Line items table ────────────────────────────────────────────────────

    private float seccionTabla(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
            Factura factura, float y) throws IOException {
        // Header row
        rect(cs, NAVY, MARGIN, y - HEAD_H, C_W, HEAD_H);
        float hY = y - HEAD_H + 6;

        txt(cs, bold, 7.5f, WHITE, MARGIN + 4, hY, "#");
        txt(cs, bold, 7.5f, WHITE, MARGIN + C_NUM + 4, hY, "DESCRIPCION");

        String cLbl = "CANT.";
        txt(cs, bold, 7.5f, WHITE, MARGIN + C_NUM + C_DESC + C_QTY - sw(bold, 7.5f, cLbl) - 4, hY, cLbl);

        String pLbl = "PRECIO UNIT.";
        txt(cs, bold, 7.5f, WHITE, MARGIN + C_NUM + C_DESC + C_QTY + C_PRICE - sw(bold, 7.5f, pLbl) - 4, hY, pLbl);

        String sLbl = "SUBTOTAL";
        txt(cs, bold, 7.5f, WHITE, PAGE_W - MARGIN - sw(bold, 7.5f, sLbl) - 4, hY, sLbl);

        y -= HEAD_H;

        List<LineaFactura> lineas = factura.getLineas();
        for (int i = 0; i < lineas.size(); i++) {
            LineaFactura linea = lineas.get(i);
            if (i % 2 == 1) {
                rect(cs, ROW_ALT, MARGIN, y - ROW_H, C_W, ROW_H);
            }

            float rY = y - ROW_H + 7;
            txt(cs, regular, 9, DARK, MARGIN + 4, rY, String.valueOf(i + 1));
            txt(cs, regular, 9, DARK, MARGIN + C_NUM + 4, rY, cut(linea.getDescripcion(), 37));

            String qty   = String.format(Locale.US, "%.2f", linea.getCantidad());
            String price = String.format(Locale.US, FMT_MONEY, linea.getPrecioUnitario());
            String sub   = String.format(Locale.US, FMT_MONEY, linea.getSubtotal());

            txt(cs, regular, 9, DARK, MARGIN + C_NUM + C_DESC + C_QTY - sw(regular, 9, qty) - 4, rY, qty);
            txt(cs, regular, 9, DARK, MARGIN + C_NUM + C_DESC + C_QTY + C_PRICE - sw(regular, 9, price) - 4, rY, price);
            txt(cs, regular, 9, DARK, PAGE_W - MARGIN - sw(regular, 9, sub) - 4, rY, sub);

            line(cs, BORDER, 0.4f, MARGIN, y - ROW_H, PAGE_W - MARGIN, y - ROW_H);
            y -= ROW_H;
        }
        return y;
    }

    // ── 5. Totals ──────────────────────────────────────────────────────────────

    private float seccionTotales(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
            Factura factura, float y) throws IOException {
        y -= 14;
        float lX = PAGE_W - MARGIN - 215f;
        float rX = PAGE_W - MARGIN;

        String stStr = String.format(Locale.US, FMT_MONEY, factura.getSubtotal());
        txt(cs, regular, 10, SECONDARY, lX, y, "Subtotal:");
        txt(cs, regular, 10, DARK, rX - sw(regular, 10, stStr), y, stStr);
        y -= 17;

        String ivaStr = String.format(Locale.US, FMT_MONEY, factura.getImpuesto());
        txt(cs, regular, 10, SECONDARY, lX, y, "IVA (13%):");
        txt(cs, regular, 10, DARK, rX - sw(regular, 10, ivaStr), y, ivaStr);
        y -= 10;

        line(cs, BORDER, 0.8f, lX, y, rX, y);
        y -= 10;

        float boxH = 28f;
        rect(cs, NAVY, lX - 6, y - boxH + 9, rX - lX + 12, boxH);

        txt(cs, bold, 10, WHITE, lX, y, "TOTAL A PAGAR:");
        String totalStr = String.format(Locale.US, FMT_MONEY, factura.getTotal());
        txt(cs, bold, 14, WHITE, rX - sw(bold, 14, totalStr), y, totalStr);

        return y - boxH;
    }

    // ── 6. Notes ───────────────────────────────────────────────────────────────

    private void seccionNotas(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
            Factura factura, float y) throws IOException {
        y -= 20;
        line(cs, BORDER, 0.5f, MARGIN, y + 12, PAGE_W - MARGIN, y + 12);
        txt(cs, bold, 8, SECONDARY, MARGIN, y, "NOTAS:");
        txt(cs, regular, 8.5f, DARK, MARGIN, y - 13, cut(factura.getNotas(), 95));
    }

    // ── 7. Footer ──────────────────────────────────────────────────────────────

    private void seccionPie(PDPageContentStream cs, PDType1Font regular) throws IOException {
        rect(cs, NAVY, 0, 0, PAGE_W, 36);
        String pie = "Serviplus S.A.  -  Gracias por su preferencia  -  Documento generado automaticamente";
        txt(cs, regular, 8, SUBTITLE, (PAGE_W - sw(regular, 8, pie)) / 2, 14, pie);
    }

    // ── Drawing primitives ─────────────────────────────────────────────────────

    private void rect(PDPageContentStream cs, Color c, float x, float y, float w, float h) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void line(PDPageContentStream cs, Color c, float lw, float x1, float y1, float x2, float y2)
            throws IOException {
        cs.setStrokingColor(c);
        cs.setLineWidth(lw);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private void txt(PDPageContentStream cs, PDType1Font font, float size, Color c, float x, float y, String val)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(c);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(val));
        cs.endText();
    }

    private float sw(PDType1Font font, float size, String text) throws IOException {
        return font.getStringWidth(safe(text)) / 1000f * size;
    }

    private String safe(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            sb.append(c <= 0xFF ? c : '?');
        }
        return sb.toString();
    }

    private String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    // ── MinIO ──────────────────────────────────────────────────────────────────

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
        String pdfUrl = "%s/%s/%s".formatted(appProperties.minio().endpoint(), bucket, objectName);
        log.info("PDF subido a MinIO: {}", pdfUrl);
        return pdfUrl;
    }
}
