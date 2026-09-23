package com.resumeai.dto;

import java.time.Instant;
import java.util.List;

public record AnalysisResponse(
        Long id,
        String fileName,
        String jobTitle,
        String companyName,
        int matchScore,
        Instant createdAt,
        boolean aiGenerated,
        List<String> strengths,
        List<String> missingSkills,
        List<String> recommendations,
        String improvedSummary
) {}
