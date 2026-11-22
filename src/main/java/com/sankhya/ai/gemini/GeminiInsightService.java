package com.sankhya.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ml.entity.GeminiInsightEntity;
import com.sankhya.ml.enums.InsightCategory;
import com.sankhya.ml.repository.GeminiInsightRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GeminiInsightService {

    private static final Logger log = LoggerFactory.getLogger(GeminiInsightService.class);

    private final GeminiClient geminiClient;
    private final GeminiPromptService promptService;
    private final GeminiInsightRepository insightRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public GeminiInsightService(GeminiClient geminiClient,
                                GeminiPromptService promptService,
                                GeminiInsightRepository insightRepository,
                                WorkspaceRepository workspaceRepository,
                                ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.promptService = promptService;
        this.insightRepository = insightRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GeminiInsightEntity generateAndPersistInsight(Long workspaceId, InsightCategory category) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        String prompt = promptService.buildPromptForCategory(workspaceId, category.name());
        String systemInstruction = promptService.getSystemInstruction();

        GeminiClient.GeminiResponse response = geminiClient.generateContent(systemInstruction, prompt);

        String narrative;
        String summary;
        int promptTokens = 0;
        int completionTokens = 0;
        String modelVersion = "DETERMINISTIC_FALLBACK";

        if (response.success()) {
            narrative = response.text();
            summary = extractSummary(narrative);
            promptTokens = response.promptTokens();
            completionTokens = response.completionTokens();
            modelVersion = response.modelUsed();
        } else {
            log.warn("Gemini generation unavailable for {}: {}. Generating heuristic fallback.", category, response.errorMessage());
            if (response.errorMessage() != null && response.errorMessage().toLowerCase().contains("not configured")) {
                modelVersion = "NOT_CONFIGURED";
            }
            narrative = generateFallbackNarrative(workspaceId, category, response.errorMessage());
            summary = "Grounded heuristic insights generated based on persisted metrics (Gemini: " + response.errorMessage() + ").";
        }

        GeminiInsightEntity entity = new GeminiInsightEntity();
        entity.setWorkspace(workspace);
        entity.setCategory(category);
        entity.setTitle(formatTitle(category));
        entity.setSummary(summary);
        entity.setFullNarrative(narrative);
        entity.setGeminiModelVersion(modelVersion);
        entity.setPromptTokens(promptTokens);
        entity.setCompletionTokens(completionTokens);
        entity.setCreatedAt(LocalDateTime.now());

        try {
            Map<String, Object> telemetry = promptService.gatherWorkspaceTelemetry(workspaceId);
            entity.setGroundedMetricsJson(objectMapper.writeValueAsString(telemetry));
        } catch (Exception ignored) {}

        return insightRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<GeminiInsightEntity> getLatestInsight(Long workspaceId, InsightCategory category) {
        return insightRepository.findFirstByWorkspaceIdAndCategoryOrderByCreatedAtDesc(workspaceId, category);
    }

    @Transactional(readOnly = true)
    public List<GeminiInsightEntity> getAllInsights(Long workspaceId) {
        return insightRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    private String extractSummary(String fullText) {
        if (fullText == null || fullText.isBlank()) return "No summary available.";
        String[] lines = fullText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("*") && trimmed.length() > 20) {
                return trimmed.length() > 250 ? trimmed.substring(0, 247) + "..." : trimmed;
            }
        }
        return fullText.length() > 250 ? fullText.substring(0, 247) + "..." : fullText;
    }

    private String formatTitle(InsightCategory category) {
        return switch (category) {
            case EXECUTIVE_SUMMARY -> "Executive Business Intelligence Summary";
            case SALES_FORECAST -> "Predictive Revenue & Seasonality Analysis";
            case CUSTOMER_CLUSTERS -> "Customer Cohort & RFM Clustering Narrative";
            case PRODUCT_AFFINITY -> "Market Basket Affinity & Cross-Sell Blueprint";
            case DEMAND_RISK -> "Inventory Stockout & Demand Spike Mitigation";
            case CHAT_CONVERSATION -> "Interactive Business Intelligence Dialogue";
        };
    }

    private String generateFallbackNarrative(Long workspaceId, InsightCategory category, String reason) {
        Map<String, Object> telemetry = promptService.gatherWorkspaceTelemetry(workspaceId);
        return String.format("""
                ## %s
                
                > **Notice:** AI generation status: %s. Displaying verified deterministic analytics summary.
                
                ### Key Telemetry Summary
                - **Total Revenue (Last 30 Days):** %s
                - **Average Basket Value:** %s
                - **Gross Margin Percentage:** %s%%
                
                ### Operational Recommendations
                1. Monitor high-velocity SKUs and replenish safety stock before anticipated weekend demand peaks.
                2. Implement co-located cross-merchandising displays for top frequent item pairs.
                3. Focus customer retention campaigns on high-monetary clusters showing increased recency days.
                """, formatTitle(category), reason != null ? reason : "Offline",
                telemetry.getOrDefault("totalRevenue", "N/A"),
                telemetry.getOrDefault("averageTransactionValue", "N/A"),
                telemetry.getOrDefault("grossMarginPercentage", "N/A"));
    }
}
