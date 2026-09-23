package com.resumeai.controller;

import com.resumeai.dto.AnalysisResponse;
import com.resumeai.dto.DashboardStatsResponse;
import com.resumeai.service.AnalysisService;
import com.resumeai.service.PdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalysisController {
    private final AnalysisService service;
    private final PdfReportService pdfReportService;

    public AnalysisController(AnalysisService service, PdfReportService pdfReportService) {
        this.service = service;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse create(
            @RequestPart("resume") MultipartFile resume,
            @RequestPart("jobTitle") String jobTitle,
            @RequestPart("companyName") String companyName,
            @RequestPart("jobDescription") String jobDescription
    ) {
        return service.create(resume, jobTitle, companyName, jobDescription);
    }

    @GetMapping("/analyses")
    public List<AnalysisResponse> all() {
        return service.all();
    }

    @GetMapping("/analyses/stats")
    public DashboardStatsResponse stats() {
        return service.stats();
    }

    @GetMapping("/analyses/{id}")
    public AnalysisResponse one(@PathVariable Long id) {
        return service.one(id);
    }

    @GetMapping(value = "/analyses/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        AnalysisResponse analysis = service.one(id);
        byte[] report = pdfReportService.create(analysis);
        String filename = safeFileName(analysis.jobTitle()) + "-resume-report.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(report);
    }

    @DeleteMapping("/analyses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) return "analysis";
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
