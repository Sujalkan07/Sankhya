package com.sankhya.analytics.customers.controller;

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
@RequestMapping("/analytics/customers")
public class CustomerIntelligenceController {

    private final WorkspaceService workspaceService;

    public CustomerIntelligenceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String showCustomerPlaceholder(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("currentPage", "customer-intelligence");

        model.addAttribute("moduleTitle", "Customer Intelligence & Churn");
        model.addAttribute("modulePhase", "Phase 3");
        model.addAttribute("moduleIcon", "group");
        model.addAttribute("moduleDescription", "Customer RFM segmentation (Recency, Frequency, Monetary value), shopper retention metrics, cohort lifecycle dynamics, and churn risk scoring.");
        model.addAttribute("moduleRoadmapDetails", "Customer registry and purchase records will be processed into automated K-Means customer clusters in Phase 5, powered by Phase 3 behavioral aggregates.");

        return "dashboard/placeholder";
    }
}
