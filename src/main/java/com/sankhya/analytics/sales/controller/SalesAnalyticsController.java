package com.sankhya.analytics.sales.controller;

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
@RequestMapping("/analytics/sales")
public class SalesAnalyticsController {

    private final WorkspaceService workspaceService;

    public SalesAnalyticsController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String showSalesPlaceholder(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("currentPage", "sales-analytics");

        model.addAttribute("moduleTitle", "Sales Analytics & Forecasting");
        model.addAttribute("modulePhase", "Phase 3");
        model.addAttribute("moduleIcon", "trending_up");
        model.addAttribute("moduleDescription", "Comprehensive revenue metrics, hourly transaction velocity, basket size trends, and sales KPIs across all POS billing registers.");
        model.addAttribute("moduleRoadmapDetails", "In Phase 2, POS datasets will be ingested. Phase 3 will activate real-time revenue KPIs, profit margins, and sales forecasting algorithms.");

        return "dashboard/placeholder";
    }
}
