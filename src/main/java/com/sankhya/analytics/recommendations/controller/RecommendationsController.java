package com.sankhya.analytics.recommendations.controller;

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
@RequestMapping("/analytics/recommendations")
public class RecommendationsController {

    private final WorkspaceService workspaceService;

    public RecommendationsController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String showRecommendationsPlaceholder(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("currentPage", "recommendations");

        model.addAttribute("moduleTitle", "Recommendation Engine & Basket Affinity");
        model.addAttribute("modulePhase", "Phase 3");
        model.addAttribute("moduleIcon", "auto_awesome");
        model.addAttribute("moduleDescription", "Market Basket Analysis, cross-selling rules, item-pair co-occurrence matrices, and bundle promotion recommendations.");
        model.addAttribute("moduleRoadmapDetails", "In Phase 3, association rule discovery and business bundling logic will be established. In Phase 5, Apriori association mining will automate product affinity optimization.");

        return "dashboard/placeholder";
    }
}
