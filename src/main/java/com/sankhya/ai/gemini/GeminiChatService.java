package com.sankhya.ai.gemini;

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

@Service
public class GeminiChatService {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatService.class);

    private final GeminiClient geminiClient;
    private final GeminiPromptService promptService;
    private final GeminiInsightRepository insightRepository;
    private final WorkspaceRepository workspaceRepository;

    public record ChatResponse(boolean success, String reply, String modelVersion, String error) {}

    public GeminiChatService(GeminiClient geminiClient,
                             GeminiPromptService promptService,
                             GeminiInsightRepository insightRepository,
                             WorkspaceRepository workspaceRepository) {
        this.geminiClient = geminiClient;
        this.promptService = promptService;
        this.insightRepository = insightRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public ChatResponse chat(Long workspaceId, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return new ChatResponse(false, "Please provide a valid question.", "", "Empty message");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            return new ChatResponse(false, "Workspace not found.", "", "Invalid workspace");
        }

        String systemInstruction = promptService.getSystemInstruction();
        String prompt = promptService.buildChatPrompt(workspaceId, userMessage);

        GeminiClient.GeminiResponse response = geminiClient.generateContent(systemInstruction, prompt);

        if (response.success()) {
            // Persist conversation item
            try {
                GeminiInsightEntity entity = new GeminiInsightEntity();
                entity.setWorkspace(workspace);
                entity.setCategory(InsightCategory.CHAT_CONVERSATION);
                entity.setTitle("Chat: " + (userMessage.length() > 60 ? userMessage.substring(0, 57) + "..." : userMessage));
                entity.setSummary(userMessage);
                entity.setFullNarrative(response.text());
                entity.setGeminiModelVersion(response.modelUsed());
                entity.setPromptTokens(response.promptTokens());
                entity.setCompletionTokens(response.completionTokens());
                entity.setCreatedAt(LocalDateTime.now());
                insightRepository.save(entity);
            } catch (Exception e) {
                log.warn("Failed to persist chat log: {}", e.getMessage());
            }

            return new ChatResponse(true, response.text(), response.modelUsed(), null);
        } else {
            return new ChatResponse(false,
                    "Unable to contact Gemini AI (" + response.errorMessage() + "). Please verify your internet connection or API key configuration in .env.",
                    "",
                    response.errorMessage());
        }
    }
}
