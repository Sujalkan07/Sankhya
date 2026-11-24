package com.sankhya.ai.gemini;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${sankhya.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${sankhya.ai.gemini.model:gemini-3.5-flash-lite}")
    private String model;

    @Value("${sankhya.ai.gemini.timeout-seconds:30}")
    private int timeoutSeconds;

    public String getApiKey() {
        return apiKey != null ? apiKey.trim() : "";
    }

    public boolean isConfigured() {
        return !getApiKey().isEmpty() && !getApiKey().equalsIgnoreCase("your_gemini_api_key_here");
    }

    public String getModel() {
        return model != null && !model.isBlank() ? model : "gemini-3.5-flash-lite";
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds > 0 ? timeoutSeconds : 30;
    }
}
