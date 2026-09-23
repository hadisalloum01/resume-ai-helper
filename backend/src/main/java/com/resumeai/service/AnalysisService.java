package com.resumeai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalysisResponse;
import com.resumeai.dto.AnalysisResult;
import com.resumeai.dto.DashboardStatsResponse;
import com.resumeai.model.Analysis;
import com.resumeai.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisService {
    private final AnalysisRepository repository;
    private final ResumeTextExtractor extractor;
    private final LocalAnalysisService local;
    private final OpenAiAnalysisService openAi;
    private final ObjectMapper mapper;

    public AnalysisService(
            AnalysisRepository repository,
            ResumeTextExtractor extractor,
            LocalAnalysisService local,
            OpenAiAnalysisService openAi,
            ObjectMapper mapper
    ) {
        this.repository = repository;
        this.extractor = extractor;
        this.local = local;
        this.openAi = openAi;
        this.mapper = mapper;
    }

    @Transactional
    public AnalysisResponse create(
            MultipartFile resume,
            String jobTitle,
            String companyName,
            String jobDescription
    ) {
        if (resume == null || resume.isEmpty()) {
            throw new IllegalArgumentException("Please select a resume.");
        }
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a job title.");
        }
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a company name.");
        }
        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a job description.");
        }

        String resumeText = extractor.extract(resume);
        AnalysisResult result;

        if (openAi.configured()) {
            try {
                result = openAi.analyze(resumeText, jobDescription);
            } catch (Exception exception) {
                result = local.analyze(resumeText, jobDescription);
            }
        } else {
            result = local.analyze(resumeText, jobDescription);
        }

        Analysis analysis = new Analysis();
        analysis.setFileName(resume.getOriginalFilename() == null ? "resume" : resume.getOriginalFilename());
        analysis.setJobTitle(jobTitle.trim());
        analysis.setCompanyName(companyName.trim());
        analysis.setResumeText(resumeText);
        analysis.setJobDescription(jobDescription.trim());
        analysis.setMatchScore(result.matchScore());
        analysis.setAiGenerated(result.aiGenerated());

        try {
            analysis.setStrengthsJson(mapper.writeValueAsString(result.strengths()));
            analysis.setMissingSkillsJson(mapper.writeValueAsString(result.missingSkills()));
            analysis.setRecommendationsJson(mapper.writeValueAsString(result.recommendations()));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not save analysis.", exception);
        }

        analysis.setImprovedSummary(result.improvedSummary());
        return toResponse(repository.save(analysis));
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> all() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AnalysisResponse one(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        List<AnalysisResponse> analyses = repository.findAll().stream().map(this::toResponse).toList();
        if (analyses.isEmpty()) {
            return new DashboardStatsResponse(0, 0, 0, List.of());
        }

        double average = analyses.stream().mapToInt(AnalysisResponse::matchScore).average().orElse(0);
        int highest = analyses.stream().mapToInt(AnalysisResponse::matchScore).max().orElse(0);

        List<String> commonMissingSkills = analyses.stream()
                .flatMap(a -> a.missingSkills().stream())
                .filter(skill -> skill != null && !skill.isBlank())
                .map(String::trim)
                .collect(Collectors.groupingBy(String::toLowerCase, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();

        return new DashboardStatsResponse(
                analyses.size(),
                Math.round(average * 10.0) / 10.0,
                highest,
                commonMissingSkills
        );
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Analysis not found.");
        }
        repository.deleteById(id);
    }

    private Analysis findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis not found."));
    }

    private AnalysisResponse toResponse(Analysis analysis) {
        try {
            return new AnalysisResponse(
                    analysis.getId(),
                    analysis.getFileName(),
                    analysis.getJobTitle() == null ? "Not provided" : analysis.getJobTitle(),
                    analysis.getCompanyName() == null ? "Not provided" : analysis.getCompanyName(),
                    analysis.getMatchScore(),
                    analysis.getCreatedAt(),
                    analysis.isAiGenerated(),
                    mapper.readValue(analysis.getStrengthsJson(), new TypeReference<List<String>>() {}),
                    mapper.readValue(analysis.getMissingSkillsJson(), new TypeReference<List<String>>() {}),
                    mapper.readValue(analysis.getRecommendationsJson(), new TypeReference<List<String>>() {}),
                    analysis.getImprovedSummary()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read saved analysis.", exception);
        }
    }
}
