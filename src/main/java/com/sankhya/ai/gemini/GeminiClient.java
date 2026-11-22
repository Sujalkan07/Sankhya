package com.sankhya.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public record GeminiResponse(
            boolean success,
            String text,
            int promptTokens,
            int completionTokens,
            String modelUsed,
            String errorMessage
    ) {
        public static GeminiResponse failure(String message) {
            return new GeminiResponse(false, "", 0, 0, "", message);
        }
    }

    public GeminiClient(GeminiConfig geminiConfig, ObjectMapper objectMapper) {
        this.geminiConfig = geminiConfig;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(geminiConfig.getTimeoutSeconds()));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(BASE_URL)
                .build();
    }

    public GeminiResponse generateContent(String systemInstruction, String userPrompt) {
        if (!geminiConfig.isConfigured()) {
            return GeminiResponse.failure("Gemini API key is not configured. Set GEMINI_API_KEY in .env or environment.");
        }

        String primaryModel = geminiConfig.getModel();
        GeminiResponse response = callGenerateContent(primaryModel, systemInstruction, userPrompt);
        
        // If 404 or model not found, try fallback model gemini-1.5-flash
        if (!response.success() && response.errorMessage() != null && response.errorMessage().contains("404")) {
            log.warn("Model {} returned 404, attempting fallback to gemini-1.5-flash", primaryModel);
            response = callGenerateContent("gemini-1.5-flash", systemInstruction, userPrompt);
        }

        return response;
    }

    private GeminiResponse callGenerateContent(String model, String systemInstruction, String userPrompt) {
        try {
            Map<String, Object> body = new HashMap<>();

            // Contents
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            userContent.put("parts", Collections.singletonList(Collections.singletonMap("text", userPrompt)));
            contents.add(userContent);
            body.put("contents", contents);

            // System Instruction
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                Map<String, Object> sysContent = new HashMap<>();
                sysContent.put("parts", Collections.singletonList(Collections.singletonMap("text", systemInstruction)));
                body.put("systemInstruction", sysContent);
            }

            // Generation Config
            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", 0.3);
            genConfig.put("maxOutputTokens", 2048);
            body.put("generationConfig", genConfig);

            String responseBody = restClient.post()
                    .uri("/models/{model}:generateContent?key={key}", model, geminiConfig.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return GeminiResponse.failure("Empty response received from Gemini API.");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode parts = firstCandidate.path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    StringBuilder text = new StringBuilder();
                    for (JsonNode part : parts) {
                        text.append(part.path("text").asText(""));
                    }

                    int promptTokens = root.path("usageMetadata").path("promptTokenCount").asInt(0);
                    int completionTokens = root.path("usageMetadata").path("candidatesTokenCount").asInt(0);

                    return new GeminiResponse(true, text.toString().trim(), promptTokens, completionTokens, model, null);
                }
            }

            return GeminiResponse.failure("No text content returned in Gemini candidates response.");

        } catch (Exception e) {
            log.error("Gemini API call failed for model {}: {}", model, e.getMessage());
            return GeminiResponse.failure("Gemini API error: " + e.getMessage());
        }
    }
}
