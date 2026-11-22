package com.sankhya.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ai.chat.NaveenStoreDataQueryService;
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
public class GeminiChatService {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatService.class);

    private final GeminiClient geminiClient;
    private final GeminiPromptService promptService;
    private final NaveenStoreDataQueryService dataQueryService;
    private final GeminiInsightRepository insightRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public record ChatResponse(boolean success, String reply, String modelVersion, String error) {}

    public record StructuredChatResponse(
            boolean success,
            String reply,
            String modelVersion,
            String intent,
            String timeWindow,
            List<Map<String, Object>> kpis,
            Map<String, Object> table,
            Map<String, Object> chart,
            Map<String, Object> meta,
            String error
    ) {}

    public GeminiChatService(GeminiClient geminiClient,
                             GeminiPromptService promptService,
                             NaveenStoreDataQueryService dataQueryService,
                             GeminiInsightRepository insightRepository,
                             WorkspaceRepository workspaceRepository,
                             ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.promptService = promptService;
        this.dataQueryService = dataQueryService;
        this.insightRepository = insightRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponse chat(Long workspaceId, String userMessage) {
        StructuredChatResponse full = chatWithContext(workspaceId, userMessage, Collections.emptyList());
        return new ChatResponse(full.success(), full.reply(), full.modelVersion(), full.error());
    }

    @Transactional
    public StructuredChatResponse chatWithContext(Long workspaceId,
                                                  String userMessage,
                                                  List<Map<String, String>> history) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return new StructuredChatResponse(false, "Please enter a question about Naveen Store.", "",
                    "INVALID", "N/A", Collections.emptyList(), null, null, Collections.emptyMap(), "Empty message");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            return new StructuredChatResponse(false, "Active Naveen Store workspace was not found.", "",
                    "INVALID", "N/A", Collections.emptyList(), null, null, Collections.emptyMap(), "Invalid workspace");
        }

        NaveenStoreDataQueryService.StructuredQueryResult verified =
                dataQueryService.executeQuery(workspaceId, userMessage);

        String finalReply;
        String modelVersion;
        int promptTokens = 0;
        int completionTokens = 0;

        if (verified.bypassLlm()) {
            finalReply = verified.deterministicNarrative();
            modelVersion = "VERIFIED_GUARDRAIL";
        } else {
            String systemInstruction = buildAnalystSystemInstruction();
            String groundedPrompt = buildGroundedAnalystPrompt(userMessage, history, verified);

            GeminiClient.GeminiResponse response = geminiClient.generateContent(systemInstruction, groundedPrompt);
            if (response.success() && response.text() != null && !response.text().isBlank()) {
                finalReply = response.text();
                modelVersion = response.modelUsed();
                promptTokens = response.promptTokens();
                completionTokens = response.completionTokens();
            } else {
                log.warn("Gemini synthesis unavailable ({}), falling back to deterministic verified summary.",
                        response.errorMessage());
                finalReply = verified.deterministicNarrative();
                modelVersion = "DETERMINISTIC_VERIFIED";
            }
        }

        try {
            GeminiInsightEntity entity = new GeminiInsightEntity();
            entity.setWorkspace(workspace);
            entity.setCategory(InsightCategory.CHAT_CONVERSATION);
            entity.setTitle("Chat: " + (userMessage.length() > 60 ? userMessage.substring(0, 57) + "..." : userMessage));
            entity.setSummary(userMessage);
            entity.setFullNarrative(finalReply);
            entity.setGeminiModelVersion(modelVersion);
            entity.setPromptTokens(promptTokens);
            entity.setCompletionTokens(completionTokens);
            entity.setCreatedAt(LocalDateTime.now());
            insightRepository.save(entity);
        } catch (Exception e) {
            log.warn("Failed to persist chat log: {}", e.getMessage());
        }

        return new StructuredChatResponse(
                true,
                finalReply,
                modelVersion,
                verified.intent().name(),
                verified.timeWindow(),
                verified.kpis(),
                verified.table(),
                verified.chart(),
                verified.meta(),
                null
        );
    }

    private String buildAnalystSystemInstruction() {
        return "You are 'Ask Naveen Store AI', the dedicated retail analytics assistant for Naveen Store (STORE-001) owned by Sujal Kansal in Lucknow, Uttar Pradesh, India.\n" +
                "STRICT RULES:\n" +
                "1. SINGLE-STORE & CURRENCY LOCK: Every answer is strictly for Naveen Store (STORE-001, Lucknow) and MUST use Indian Rupees (₹), never $.\n" +
                "2. VERIFIED GROUNDING ONLY: Quote ONLY exact figures from VERIFIED_QUERY_RESULT JSON. Never invent numbers, customer emails/phones, historical daily inventory movements, or external store comparisons.\n" +
                "3. PROVENANCE HONESTY: Note relevant dataset scope/limitations concisely when applicable (synthetic INR financials, reconstructed 2025-11-22 to 2026-11-22 dates, Lucknow standardized city, top-1,000 Instacart-derived products, single 2026-11-22 inventory snapshot).\n" +
                "4. FORMAT: Write a clear, concise executive summary with bullet points in Markdown (headings, bold key metrics) that complements the KPI cards, data table, and chart rendered right below your answer.";
    }

    private String buildGroundedAnalystPrompt(String userMessage,
                                              List<Map<String, String>> history,
                                              NaveenStoreDataQueryService.StructuredQueryResult verified) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("intent", verified.intent().name());
        payload.put("timeWindow", verified.timeWindow());
        payload.put("meta", verified.meta());
        payload.put("kpis", verified.kpis());
        payload.put("table", verified.table());
        payload.put("baselineSummary", verified.deterministicNarrative());

        String json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (Exception e) {
            json = payload.toString();
        }

        StringBuilder sb = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            sb.append("RECENT SESSION TURNS:\n");
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> turn = history.get(i);
                sb.append(turn.getOrDefault("role", "user")).append(": ")
                        .append(turn.getOrDefault("text", "")).append("\n");
            }
            sb.append("\n");
        }
        sb.append("USER QUESTION: \"").append(userMessage).append("\"\n\n");
        sb.append("VERIFIED_QUERY_RESULT (Naveen Store STORE-001, Lucknow, INR):\n```json\n")
                .append(json).append("\n```");
        return sb.toString();
    }
}
