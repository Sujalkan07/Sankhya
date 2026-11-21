package com.sankhya.analytics.products.controller;

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
@RequestMapping("/analytics/products")
public class ProductIntelligenceController {

    private final WorkspaceService workspaceService;

    public ProductIntelligenceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String showProductPlaceholder(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("currentPage", "product-intelligence");

        model.addAttribute("moduleTitle", "Product Intelligence & SKU Profitability");
        model.addAttribute("modulePhase", "Phase 3");
        model.addAttribute("moduleIcon", "category");
        model.addAttribute("moduleDescription", "SKU-level gross margin analysis, sales velocity rankings, category sales contribution, and slow-moving inventory alerts.");
        model.addAttribute("moduleRoadmapDetails", "Catalog classification and margin models will link transactional sales to catalog unit economics in Phase 3.");

        return "dashboard/placeholder";
    }
}
