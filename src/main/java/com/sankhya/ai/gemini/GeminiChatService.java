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
        return "You are 'Ask Naveen Store AI', the intelligent retail analytics AI consultant for Naveen Store (STORE-001) owned by Sujal Kansal in Lucknow, Uttar Pradesh, India.\n" +
                "GUIDELINES:\n" +
                "1. SINGLE-STORE & INR CURRENCY: You represent Naveen Store (STORE-001, Lucknow, UP). All monetary figures MUST be formatted in Indian Rupees (₹), never dollars ($).\n" +
                "2. DIRECT & SPECIFIC ANSWERS: Answer the user's specific question directly, insightfully, and conversationally. If the user asks about top customers, name the specific top customers, their spend, and order frequency from the provided table. If they ask about products, highlight the top SKUs and their revenue.\n" +
                "3. ACCURATE GROUNDING: Quote and ground your analysis in the figures, customer profiles, product names, and categories present in VERIFIED_QUERY_RESULT. Do not invent contradictory figures.\n" +
                "4. STRATEGIC BUSINESS VALUE: Provide practical, actionable retail recommendations for store owner Sujal Kansal to optimize inventory, retain top shoppers, and boost margins.\n" +
                "5. NATURAL PROFESSIONAL TONE: Speak naturally and professionally as a senior supermarket data consultant. Do NOT include generic disclaimers or repetitive technical caveats unless explicitly asked about dataset architecture.\n" +
                "6. FORMAT: Structure your answer using clean Markdown (clear section headings, bold metrics, and concise bullet points) that pairs with the KPI cards, table, and chart displayed below your response.";
    }

    private String buildGroundedAnalystPrompt(String userMessage,
                                              List<Map<String, String>> history,
                                              NaveenStoreDataQueryService.StructuredQueryResult verified) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("store", "Naveen Store (STORE-001), Lucknow, Uttar Pradesh");
        payload.put("owner", "Sujal Kansal");
        payload.put("intent", verified.intent().name());
        payload.put("timeWindow", verified.timeWindow());
        payload.put("kpis", verified.kpis());
        payload.put("table", verified.table());
        payload.put("chart", verified.chart());
        if (verified.deterministicNarrative() != null && !verified.deterministicNarrative().isBlank()) {
            payload.put("telemetrySummary", verified.deterministicNarrative());
        }

        String json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (Exception e) {
            json = payload.toString();
        }

        StringBuilder sb = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            sb.append("CONVERSATION HISTORY:\n");
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> turn = history.get(i);
                sb.append(turn.getOrDefault("role", "user")).append(": ")
                        .append(turn.getOrDefault("text", "")).append("\n");
            }
            sb.append("\n");
        }
        sb.append("STORE OWNER QUESTION: \"").append(userMessage).append("\"\n\n");
        sb.append("VERIFIED_QUERY_RESULT (Naveen Store STORE-001, Lucknow, INR ₹):\n```json\n")
                .append(json).append("\n```\n\n");
        sb.append("Analyze the verified store data above and provide a comprehensive, intelligent answer to the owner's question. Directly highlight the specific customers, products, or metrics requested.");
        return sb.toString();
    }
}
