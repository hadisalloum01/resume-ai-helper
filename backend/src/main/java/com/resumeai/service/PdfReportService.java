package com.resumeai.service;

import com.resumeai.dto.AnalysisResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfReportService {
    private static final float MARGIN = 50;
    private static final float WIDTH = PDRectangle.A4.getWidth() - (MARGIN * 2);
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public byte[] create(AnalysisResponse analysis) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Writer writer = new Writer(document);
            writer.heading("Resume Match Report", 20);
            writer.line("Job title: " + safe(analysis.jobTitle()), 11, true);
            writer.line("Company: " + safe(analysis.companyName()), 11, false);
            writer.line("Resume: " + safe(analysis.fileName()), 11, false);
            writer.line("Match score: " + analysis.matchScore() + "%", 14, true);
            writer.space(8);
            writer.section("Strengths", analysis.strengths());
            writer.section("Missing Skills", analysis.missingSkills());
            writer.section("Recommendations", analysis.recommendations());
            writer.heading("Improved Professional Summary", 14);
            writer.paragraph(safe(analysis.improvedSummary()), 11);
            writer.close();
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate PDF report.", exception);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }

    private static final class Writer {
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        private Writer(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private void ensure(float required) throws IOException {
            if (y - required < MARGIN) newPage();
        }

        private void heading(String text, float size) throws IOException {
            ensure(size + 18);
            writeLine(text, BOLD, size, 0);
            y -= 8;
        }

        private void line(String text, float size, boolean bold) throws IOException {
            ensure(size + 8);
            writeLine(text, bold ? BOLD : REGULAR, size, 0);
        }

        private void space(float points) { y -= points; }

        private void section(String title, List<String> items) throws IOException {
            heading(title, 14);
            List<String> values = items == null || items.isEmpty() ? List.of("None identified.") : items;
            for (String item : values) paragraph("- " + item, 11);
            y -= 5;
        }

        private void paragraph(String text, float size) throws IOException {
            for (String line : wrap(text, size)) {
                ensure(size + 6);
                writeLine(line, REGULAR, size, 0);
            }
        }

        private void writeLine(String text, PDType1Font font, float size, float indent) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN + indent, y);
            stream.showText(sanitize(text));
            stream.endText();
            y -= size + 5;
        }

        private List<String> wrap(String text, float fontSize) throws IOException {
            String clean = sanitize(text);
            if (clean.isBlank()) return List.of("");
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : clean.split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float candidateWidth = REGULAR.getStringWidth(candidate) / 1000 * fontSize;
                if (candidateWidth > WIDTH && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
            return lines;
        }

        private String sanitize(String text) {
            if (text == null) return "";
            return text.replaceAll("[^\\x20-\\x7E]", " ").replaceAll("\\s+", " ").trim();
        }

        private void close() throws IOException {
            if (stream != null) stream.close();
        }
    }
}
