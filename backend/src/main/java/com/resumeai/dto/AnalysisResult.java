package com.resumeai.dto;

import java.util.List;

public record AnalysisResult(int matchScore, List<String> strengths, List<String> missingSkills,
                             List<String> recommendations, String improvedSummary, boolean aiGenerated) {}
