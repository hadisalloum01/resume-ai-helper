package com.resumeai.dto;

import java.util.List;

public record DashboardStatsResponse(
        long totalAnalyses,
        double averageMatchScore,
        int highestMatchScore,
        List<String> mostCommonMissingSkills
) {}
