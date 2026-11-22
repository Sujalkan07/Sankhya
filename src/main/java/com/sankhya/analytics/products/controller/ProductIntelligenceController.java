package com.sankhya.analytics.products.controller;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.products.dto.ProductIntelligenceResponse;
import com.sankhya.analytics.products.service.ProductIntelligenceService;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.ml.entity.AssociationRuleEntity;
import com.sankhya.ml.entity.DemandPredictionEntity;
import com.sankhya.ml.repository.AssociationRuleRepository;
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
@RequestMapping("/analytics/products")
public class ProductIntelligenceController {

    private final WorkspaceService workspaceService;
    private final ProductIntelligenceService productIntelligenceService;
    private final DatasetQueryService datasetQueryService;
    private final AssociationRuleRepository associationRuleRepository;
    private final DemandPredictionRepository demandPredictionRepository;

    public ProductIntelligenceController(WorkspaceService workspaceService,
                                         ProductIntelligenceService productIntelligenceService,
                                         DatasetQueryService datasetQueryService,
                                         AssociationRuleRepository associationRuleRepository,
                                         DemandPredictionRepository demandPredictionRepository) {
        this.workspaceService = workspaceService;
        this.productIntelligenceService = productIntelligenceService;
        this.datasetQueryService = datasetQueryService;
        this.associationRuleRepository = associationRuleRepository;
        this.demandPredictionRepository = demandPredictionRepository;
    }

    @GetMapping
    public String showProductIntelligence(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("activeModule", "products");
        model.addAttribute("activeSubPage", "overview");
        model.addAttribute("currentPage", "product-intelligence");

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        model.addAttribute("filter", filter);
        model.addAttribute("currentPreset", preset);
        model.addAttribute("allPresets", DatePreset.values());

        if (activeWorkspace != null) {
            Long wsId = activeWorkspace.getId();
            AnalyticsReadinessResponse readiness = datasetQueryService.getAnalyticsReadiness(wsId);
            model.addAttribute("readiness", readiness);

            if (readiness.isSalesReady()) {
                ProductIntelligenceResponse response = productIntelligenceService.getProductIntelligence(wsId, filter);
                model.addAttribute("analytics", response);
            }

            // Phase 5 ML Association Rules & Demand Predictions
            List<AssociationRuleEntity> rules = associationRuleRepository.findTop20ByWorkspaceIdOrderByLiftDesc(wsId);
            model.addAttribute("associationRules", rules);

            List<DemandPredictionEntity> demandPredictions = demandPredictionRepository.findByWorkspaceIdOrderByPredictedUnitsNext7dDesc(wsId);
            model.addAttribute("demandPredictions", demandPredictions);
        }

        return "analytics/products";
    }

    @GetMapping("/api")
    @ResponseBody
    public ProductIntelligenceResponse getProductApi(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    HttpSession session,
                                                    @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) return new ProductIntelligenceResponse();

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        return productIntelligenceService.getProductIntelligence(activeWorkspace.getId(), filter);
    }
}
