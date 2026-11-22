package com.sankhya.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    private static final String VERIFIED_FALLBACK_MODEL = "gemini-3.5-flash-lite";
    private static final String SECONDARY_FALLBACK_MODEL = "gemini-3-flash-preview";

    public GeminiResponse generateContent(String systemInstruction, String userPrompt) {
        if (!geminiConfig.isConfigured()) {
            return GeminiResponse.failure("Gemini API key is not configured. Set GEMINI_API_KEY in environment.");
        }

        String primaryModel = geminiConfig.getModel();
        GeminiResponse response = callGenerateContent(primaryModel, systemInstruction, userPrompt);

        // Retry once with verified primary fallback model on 404/503 unavailable errors
        if (!response.success()
                && !VERIFIED_FALLBACK_MODEL.equalsIgnoreCase(primaryModel)
                && isModelUnavailable(response.errorMessage())) {
            log.warn("Primary model {} unavailable ({}), attempting fallback to {}",
                    primaryModel, sanitizeMessage(response.errorMessage()), VERIFIED_FALLBACK_MODEL);
            response = callGenerateContent(VERIFIED_FALLBACK_MODEL, systemInstruction, userPrompt);
        }

        // Secondary fallback on transient 503/404 using live-verified gemini-3-flash-preview
        if (!response.success()
                && !SECONDARY_FALLBACK_MODEL.equalsIgnoreCase(primaryModel)
                && isModelUnavailable(response.errorMessage())) {
            log.warn("Gemini model {} unavailable ({}), attempting secondary fallback to {}",
                    primaryModel, sanitizeMessage(response.errorMessage()), SECONDARY_FALLBACK_MODEL);
            response = callGenerateContent(SECONDARY_FALLBACK_MODEL, systemInstruction, userPrompt);
        }

        return response;
    }

    private boolean isModelUnavailable(String errorMessage) {
        return errorMessage != null
                && (errorMessage.contains("MODEL_UNAVAILABLE") || errorMessage.contains("404") || errorMessage.contains("503"));
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
                return GeminiResponse.failure("MALFORMED_RESPONSE: Empty response received from Gemini API.");
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

            return GeminiResponse.failure("MALFORMED_RESPONSE: No text content returned in Gemini candidates response.");

        } catch (HttpClientErrorException ex) {
            int code = ex.getStatusCode().value();
            String category = switch (code) {
                case 401, 403 -> "AUTH_ERROR (" + code + "): Invalid or unauthorized Gemini API key.";
                case 429 -> "QUOTA_EXCEEDED (429): Gemini API rate limit or quota exceeded.";
                case 404 -> "MODEL_UNAVAILABLE (404): Model '" + model + "' not found or deprecated.";
                default -> "CLIENT_ERROR (" + code + "): " + sanitizeMessage(ex.getStatusText());
            };
            log.error("Gemini HTTP client error for model {}: {}", model, category);
            return GeminiResponse.failure(category);
        } catch (HttpServerErrorException ex) {
            int code = ex.getStatusCode().value();
            String msg = "MODEL_UNAVAILABLE (" + code + "): Gemini service temporarily unavailable.";
            log.error("Gemini HTTP server error for model {}: {}", model, msg);
            return GeminiResponse.failure(msg);
        } catch (ResourceAccessException ex) {
            String msg = "NETWORK_TIMEOUT: Unable to reach Gemini API (" + sanitizeMessage(ex.getMessage()) + ").";
            log.error("Gemini network/timeout error for model {}: {}", model, msg);
            return GeminiResponse.failure(msg);
        } catch (Exception e) {
            String safeMsg = sanitizeMessage(e.getMessage());
            log.error("Gemini API call failed for model {}: {}", model, safeMsg);
            return GeminiResponse.failure("Gemini API error: " + safeMsg);
        }
    }

    private String sanitizeMessage(String raw) {
        if (raw == null) return "Unknown error";
        return raw.replaceAll("(?i)(key=)[^&\\s\"']+", "$1****");
    }
}
