package com.aichecker.service.impl;

import com.aichecker.service.AiSemanticService;
import com.aichecker.service.SemanticEvaluation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class AiSemanticServiceImpl implements AiSemanticService {

    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-z0-9 ]");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.openai-api-key:}")
    private String openAiApiKey;

    @Value("${app.ai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.ai.subjective-threshold:0.72}")
    private double threshold;

    public AiSemanticServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public SemanticEvaluation evaluate(String expectedAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return new SemanticEvaluation(false, 0.0, "No answer submitted", false);
        }

        SemanticEvaluation apiResult = evaluateWithOpenAi(expectedAnswer, studentAnswer);
        if (apiResult != null) {
            return apiResult;
        }
        return evaluateFallback(expectedAnswer, studentAnswer);
    }

    private SemanticEvaluation evaluateWithOpenAi(String expectedAnswer, String studentAnswer) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.0);
            payload.put("response_format", Map.of("type", "json_object"));
            payload.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "You are an exam evaluator. Compare expected and student answers semantically. " +
                                    "Return strict JSON with fields: score (0 to 1), explanation (max 20 words)."),
                    Map.of("role", "user", "content",
                            "Expected Answer: " + expectedAnswer + "\n" +
                                    "Student Answer: " + studentAnswer + "\n" +
                                    "Score semantic correctness from 0 to 1.")
            ));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(openAiApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                return null;
            }

            JsonNode eval = objectMapper.readTree(contentNode.asText());
            double score = clamp(eval.path("score").asDouble(0.0));
            String explanation = eval.path("explanation").asText("Semantic comparison completed");
            boolean correct = score >= threshold;
            return new SemanticEvaluation(correct, score, explanation, true);
        } catch (Exception ex) {
            return null;
        }
    }

    private SemanticEvaluation evaluateFallback(String expectedAnswer, String studentAnswer) {
        String normalizedExpected = normalize(expectedAnswer);
        String normalizedStudent = normalize(studentAnswer);

        if (normalizedExpected.equals(normalizedStudent)) {
            return new SemanticEvaluation(true, 1.0, "Exact match (case-insensitive)", false);
        }

        Set<String> expectedTokens = new HashSet<>(Arrays.asList(normalizedExpected.split("\\s+")));
        Set<String> studentTokens = new HashSet<>(Arrays.asList(normalizedStudent.split("\\s+")));
        expectedTokens.removeIf(String::isBlank);
        studentTokens.removeIf(String::isBlank);

        Set<String> intersection = new HashSet<>(expectedTokens);
        intersection.retainAll(studentTokens);

        Set<String> union = new HashSet<>(expectedTokens);
        union.addAll(studentTokens);

        double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

        int maxLen = Math.max(normalizedExpected.length(), normalizedStudent.length());
        int distance = LevenshteinDistance.getDefaultInstance().apply(normalizedExpected, normalizedStudent);
        double levenshteinScore = maxLen == 0 ? 0.0 : 1.0 - ((double) distance / maxLen);

        double score = clamp(Math.max(jaccard, levenshteinScore));
        boolean correct = score >= threshold;
        String explanation = correct
                ? "Semantically similar based on fallback lexical match"
                : "Low semantic similarity; key concepts missing";

        return new SemanticEvaluation(correct, score, explanation, false);
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        String cleaned = NON_ALPHANUM.matcher(input.toLowerCase(Locale.ROOT)).replaceAll(" ");
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }
}
