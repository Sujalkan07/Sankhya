package com.sankhya.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ai.gemini.GeminiClient;
import com.sankhya.ai.gemini.GeminiConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeminiClient Unit Tests")
class GeminiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GeminiClient gracefully reports unconfigured key when apiKey is missing")
    void testUnconfiguredApiKey() {
        GeminiConfig config = new GeminiConfig();
        ReflectionTestUtils.setField(config, "apiKey", "");
        ReflectionTestUtils.setField(config, "model", "gemini-2.5-flash");

        GeminiClient client = new GeminiClient(config, objectMapper);
        GeminiClient.GeminiResponse response = client.generateContent("System", "User query");

        assertNotNull(response);
        assertFalse(response.success());
        assertTrue(response.errorMessage().contains("not configured"));
    }

    @Test
    @DisplayName("GeminiConfig defaults model to gemini-2.5-flash")
    void testGeminiConfigDefaults() {
        GeminiConfig config = new GeminiConfig();
        assertEquals("gemini-2.5-flash", config.getModel());
        assertEquals(30, config.getTimeoutSeconds());
        assertFalse(config.isConfigured());
    }
}
