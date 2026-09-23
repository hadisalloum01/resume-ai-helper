package com.resumeai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class ResumeTextExtractor {
    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please upload a resume file.");
        String name = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
        String lower = name.toLowerCase(Locale.ROOT);
        try {
            String text;
            if (lower.endsWith(".pdf")) {
                try (var document = Loader.loadPDF(file.getBytes())) { text = new PDFTextStripper().getText(document); }
            } else if (lower.endsWith(".docx")) {
                try (InputStream in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {
                    text = doc.getParagraphs().stream().map(p -> p.getText()).reduce("", (a,b) -> a + "\n" + b);
                }
            } else if (lower.endsWith(".txt")) {
                text = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else {
                throw new IllegalArgumentException("Only PDF, DOCX, and TXT files are supported.");
            }
            text = text.replaceAll("[\t ]+", " ").replaceAll("\n{3,}", "\n\n").trim();
            if (text.length() < 30) throw new IllegalArgumentException("The resume does not contain enough readable text.");
            return text;
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("Could not read the uploaded resume.", e); }
    }
}
