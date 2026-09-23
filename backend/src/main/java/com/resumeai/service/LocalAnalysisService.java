package com.resumeai.service;

import com.resumeai.dto.AnalysisResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LocalAnalysisService {
    private static final Set<String> STOP = Set.of("and","the","with","for","that","this","from","your","you","our","are","will","have","has","job","role","work","years","year","into","using","skills","experience","team","who","but","not","all","can","their","they","its","about","responsibilities","requirements","preferred");
    private static final Pattern WORD = Pattern.compile("[a-zA-Z][a-zA-Z0-9+#.-]{1,}");

    public AnalysisResult analyze(String resume, String job) {
        Set<String> resumeWords = keywords(resume);
        List<String> jobTerms = keywords(job).stream().limit(40).toList();
        List<String> matched = jobTerms.stream().filter(resumeWords::contains).limit(8).toList();
        List<String> missing = jobTerms.stream().filter(t -> !resumeWords.contains(t)).limit(8).toList();
        int score = jobTerms.isEmpty() ? 0 : (int)Math.round(100.0 * matched.size() / Math.min(jobTerms.size(), 12));
        score = Math.max(15, Math.min(95, score));
        List<String> strengths = matched.isEmpty()
                ? List.of("The resume contains transferable professional experience.", "The document provides a foundation that can be tailored to the role.")
                : matched.stream().limit(5).map(t -> "Relevant experience or knowledge related to " + display(t) + ".").toList();
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Add measurable outcomes to recent experience, such as percentages, time saved, or volume handled.");
        if (!missing.isEmpty()) recommendations.add("Add truthful examples demonstrating: " + missing.stream().limit(4).map(this::display).collect(Collectors.joining(", ")) + ".");
        recommendations.add("Mirror important job-description terminology naturally in the summary and experience sections.");
        recommendations.add("Keep the resume concise and place the most relevant technical skills near the top.");
        String top = matched.stream().limit(4).map(this::display).collect(Collectors.joining(", "));
        String summary = "Results-oriented professional with practical experience" + (top.isBlank() ? " in software and data-focused work" : " in " + top) + ". Skilled at solving problems, collaborating with teams, and delivering reliable outcomes. Eager to apply a strong technical foundation and continuous-learning mindset to this opportunity.";
        return new AnalysisResult(score, strengths, missing.stream().map(this::display).toList(), recommendations, summary, false);
    }

    private Set<String> keywords(String text) {
        var m = WORD.matcher(text.toLowerCase(Locale.ROOT));
        Map<String,Integer> counts = new HashMap<>();
        while(m.find()) { String w=m.group(); if(w.length()>2 && !STOP.contains(w)) counts.merge(w,1,Integer::sum); }
        return counts.entrySet().stream().sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
    }
    private String display(String s) { return Arrays.stream(s.split("[-.]"))
            .map(p -> p.isEmpty()?p:Character.toUpperCase(p.charAt(0))+p.substring(1)).collect(Collectors.joining(" ")); }
}
