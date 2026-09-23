package com.resumeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.dto.AnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class OpenAiAnalysisService {
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper;
    private final RestClient client;

    public OpenAiAnalysisService(@Value("${openai.api-key:}") String apiKey,
                                 @Value("${openai.model:gpt-4.1-mini}") String model,
                                 ObjectMapper mapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.mapper = mapper;
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1").build();
    }

    public boolean configured() { return !apiKey.isBlank(); }

    public AnalysisResult analyze(String resume, String job) {
        String prompt = """
                Analyze the resume against the job description. Return ONLY valid JSON with this exact shape:
                {"matchScore":0,"strengths":[""],"missingSkills":[""],"recommendations":[""],"improvedSummary":""}
                Match score must be an integer from 0 to 100. Be accurate and do not claim skills not present in the resume.

                RESUME:
                %s

                JOB DESCRIPTION:
                %s
                """.formatted(limit(resume, 14000), limit(job, 10000));
        Map<String,Object> body = Map.of(
                "model", model,
                "input", prompt,
                "temperature", 0.2
        );
        JsonNode response = client.post().uri("/responses")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(JsonNode.class);
        try {
            String text = extractText(response);
            text = text.replaceFirst("(?s)^```(?:json)?\\s*", "").replaceFirst("(?s)\\s*```$", "").trim();
            JsonNode n = mapper.readTree(text);
            return new AnalysisResult(
                    Math.max(0, Math.min(100, n.path("matchScore").asInt())),
                    strings(n.path("strengths")), strings(n.path("missingSkills")),
                    strings(n.path("recommendations")), n.path("improvedSummary").asText(), true);
        } catch (Exception e) { throw new IllegalStateException("OpenAI returned an unreadable response.", e); }
    }

    private String extractText(JsonNode response) {
        if (response == null) throw new IllegalStateException("OpenAI returned no response.");
        if (response.hasNonNull("output_text")) return response.get("output_text").asText();
        for (JsonNode item : response.path("output"))
            for (JsonNode content : item.path("content"))
                if (content.hasNonNull("text")) return content.get("text").asText();
        throw new IllegalStateException("OpenAI response did not contain text.");
    }
    private List<String> strings(JsonNode n) { List<String> out=new ArrayList<>(); if(n.isArray()) n.forEach(x -> out.add(x.asText())); return out; }
    private String limit(String s, int max) { return s.length() <= max ? s : s.substring(0,max); }
}
