package com.sankhya.analytics.ai.controller;

import com.sankhya.security.CustomUserDetails;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/analytics/ai-insights")
public class AiInsightsController {

    private final WorkspaceService workspaceService;

    public AiInsightsController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String showAiInsightsPlaceholder(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            HttpSession session,
                                            Model model) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(
                userDetails.getId(),
                activeWorkspace != null ? activeWorkspace.getId() : null
        );

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("activeWorkspace", activeWorkspace);
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("currentPage", "ai-insights");

        model.addAttribute("moduleTitle", "AI Insights & Executive Explanations");
        model.addAttribute("modulePhase", "Phase 5");
        model.addAttribute("moduleIcon", "psychology");
        model.addAttribute("moduleDescription", "Natural language explanations, automated executive briefs, prescriptive retail recommendations, and Gemini-grounded anomaly analysis.");
        model.addAttribute("moduleRoadmapDetails", "In Phase 5, numerical computations and Weka algorithms will first compute statistical realities; Gemini will then explain these analytical facts strictly without data hallucination.");

        return "dashboard/placeholder";
    }
}
