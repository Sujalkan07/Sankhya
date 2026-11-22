package com.sankhya.analytics.inventory.controller;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.inventory.dto.InventoryIntelligenceResponse;
import com.sankhya.analytics.inventory.service.InventoryIntelligenceService;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.ml.entity.DemandPredictionEntity;
import com.sankhya.ml.repository.DemandPredictionRepository;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/analytics/inventory")
public class InventoryAnalyticsController {

    private final WorkspaceService workspaceService;
    private final InventoryIntelligenceService inventoryIntelligenceService;
    private final DatasetQueryService datasetQueryService;
    private final DemandPredictionRepository demandPredictionRepository;

    public InventoryAnalyticsController(WorkspaceService workspaceService,
                                        InventoryIntelligenceService inventoryIntelligenceService,
                                        DatasetQueryService datasetQueryService,
                                        DemandPredictionRepository demandPredictionRepository) {
        this.workspaceService = workspaceService;
        this.inventoryIntelligenceService = inventoryIntelligenceService;
        this.datasetQueryService = datasetQueryService;
        this.demandPredictionRepository = demandPredictionRepository;
    }

    @GetMapping
    public String showInventoryAnalytics(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         HttpSession session,
                                         @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         Model model) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        List<WorkspaceResponse> workspaces = workspaceService.getUserWorkspaces(
                userDetails.getId(),
                activeWorkspace != null ? activeWorkspace.getId() : null
        );

        model.addAttribute("currentUser", userDetails);
        model.addAttribute("activeWorkspace", activeWorkspace);
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("activeModule", "inventory");
        model.addAttribute("activeSubPage", "overview");
        model.addAttribute("currentPage", "inventory");

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        model.addAttribute("filter", filter);
        model.addAttribute("currentPreset", preset);
        model.addAttribute("allPresets", DatePreset.values());

        if (activeWorkspace != null) {
            Long wsId = activeWorkspace.getId();
            AnalyticsReadinessResponse readiness = datasetQueryService.getAnalyticsReadiness(wsId);
            model.addAttribute("readiness", readiness);

            if (readiness.isInventoryReady()) {
                InventoryIntelligenceResponse response = inventoryIntelligenceService.getInventoryIntelligence(wsId, filter);
                model.addAttribute("analytics", response);
            }

            // Phase 5 ML Demand & Stockout Risk Predictions
            List<DemandPredictionEntity> demandPredictions = demandPredictionRepository.findByWorkspaceIdOrderByPredictedUnitsNext7dDesc(wsId);
            model.addAttribute("demandPredictions", demandPredictions);
        }

        return "analytics/inventory";
    }

    @GetMapping("/api")
    @ResponseBody
    public InventoryIntelligenceResponse getInventoryApi(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        HttpSession session,
                                                        @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) return new InventoryIntelligenceResponse();

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        return inventoryIntelligenceService.getInventoryIntelligence(activeWorkspace.getId(), filter);
    }
}
