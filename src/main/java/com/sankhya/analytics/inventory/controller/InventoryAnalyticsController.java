package com.sankhya.analytics.inventory.controller;

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
@RequestMapping("/analytics/inventory")
public class InventoryAnalyticsController {

    private final WorkspaceService workspaceService;

    public InventoryAnalyticsController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String showInventoryPlaceholder(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("currentPage", "inventory");

        model.addAttribute("moduleTitle", "Inventory Intelligence & Stockout Prediction");
        model.addAttribute("modulePhase", "Phase 3");
        model.addAttribute("moduleIcon", "inventory_2");
        model.addAttribute("moduleDescription", "Stock drift, lead-time velocity, reorder point alerts, buffer stock recommendations, and perishable shrinkage mitigation.");
        model.addAttribute("moduleRoadmapDetails", "Daily warehouse and POS stock logs ingested in Phase 2 will be analyzed in Phase 3 for reorder thresholds and safety stock levels.");

        return "dashboard/placeholder";
    }
}
