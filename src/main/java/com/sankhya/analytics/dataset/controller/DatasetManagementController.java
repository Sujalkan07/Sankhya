package com.sankhya.analytics.dataset.controller;

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
@RequestMapping("/datasets")
public class DatasetManagementController {

    private final WorkspaceService workspaceService;

    public DatasetManagementController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public String showDatasetPlaceholder(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("currentPage", "dataset-management");

        model.addAttribute("moduleTitle", "Dataset Management & Ingestion Pipeline");
        model.addAttribute("modulePhase", "Phase 2 Next");
        model.addAttribute("moduleIcon", "table_chart");
        model.addAttribute("moduleDescription", "Multi-table CSV and Excel ingestion (Sales Transactions, Customer Profiles, Product Catalog, Inventory Stock), data validation, schema mapping, and pre-processing.");
        model.addAttribute("moduleRoadmapDetails", "Phase 2 introduces Apache POI, OpenCSV, automated schema validation, missing-value handling, and synthetic Indian supermarket data spanning 2-3 years.");

        return "dashboard/placeholder";
    }
}
