package com.eletroflow.plugin.service;

import com.eletroflow.plugin.config.EfiSettings;
import com.eletroflow.plugin.model.PaymentCheckResult;
import com.eletroflow.plugin.model.PaymentRecord;
import com.eletroflow.plugin.model.PlanRecord;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class ReceiptPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withLocale(new Locale("pt", "BR"))
            .withZone(ZoneId.of("America/Sao_Paulo"));
    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final EfiSettings efiSettings;

    public ReceiptPdfService(EfiSettings efiSettings) {
        this.efiSettings = efiSettings;
    }

    public byte[] generate(PaymentRecord payment, PlanRecord plan, PaymentCheckResult result) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float left = 56f;
                float top = page.getMediaBox().getHeight() - 72f;
                float lineHeight = 18f;
                write(contentStream, FONT_BOLD, 18f, left, top, "Comprovante Pix");
                top -= 32f;
                write(contentStream, FONT_BOLD, 11f, left, top, "Recebedor");
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Nome: " + fallback(efiSettings.receiverName()));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Documento: " + fallback(efiSettings.receiverDocument()));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Chave Pix: " + fallback(efiSettings.pixKey()));
                top -= 28f;
                write(contentStream, FONT_BOLD, 11f, left, top, "Pagador");
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Nome: " + fallback(payment.payerName()));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "CPF: " + fallback(payment.payerCpf()));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Conta Minecraft: " + fallback(payment.minecraftUsername()));
                top -= 28f;
                write(contentStream, FONT_BOLD, 11f, left, top, "Pagamento");
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "VIP: " + fallback(plan.displayName()));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Grupo LuckPerms: " + fallback(plan.luckPermsGroup()));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Valor: " + payment.amount() + " " + plan.currency());
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "TXID: " + fallback(payment.txid()));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "EndToEnd: " + fallback(result.endToEndId()));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Confirmado em: " + format(result));
                top -= lineHeight;
                write(contentStream, FONT_REGULAR, 11f, left, top, "Duracao do VIP: " + plan.durationDays() + " dias");
            }
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate Pix receipt PDF", exception);
        }
    }

    private void write(PDPageContentStream contentStream, PDFont font, float size, float x, float y, String text) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, size);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String format(PaymentCheckResult result) {
        if (result.confirmedAt() == null) {
            return "-";
        }
        return DATE_TIME_FORMATTER.format(result.confirmedAt());
    }
}
