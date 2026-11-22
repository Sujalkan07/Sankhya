package com.sankhya.analytics.customers.controller;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.customers.dto.CustomerIntelligenceResponse;
import com.sankhya.analytics.customers.service.CustomerIntelligenceService;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.ml.entity.CustomerClusterAssignment;
import com.sankhya.ml.repository.CustomerClusterAssignmentRepository;
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
@RequestMapping("/analytics/customers")
public class CustomerIntelligenceController {

    private final WorkspaceService workspaceService;
    private final CustomerIntelligenceService customerIntelligenceService;
    private final DatasetQueryService datasetQueryService;
    private final CustomerClusterAssignmentRepository customerClusterAssignmentRepository;

    public CustomerIntelligenceController(WorkspaceService workspaceService,
                                          CustomerIntelligenceService customerIntelligenceService,
                                          DatasetQueryService datasetQueryService,
                                          CustomerClusterAssignmentRepository customerClusterAssignmentRepository) {
        this.workspaceService = workspaceService;
        this.customerIntelligenceService = customerIntelligenceService;
        this.datasetQueryService = datasetQueryService;
        this.customerClusterAssignmentRepository = customerClusterAssignmentRepository;
    }

    @GetMapping
    public String showCustomerIntelligence(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("activeModule", "customers");
        model.addAttribute("activeSubPage", "overview");
        model.addAttribute("currentPage", "customer-intelligence");

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        model.addAttribute("filter", filter);
        model.addAttribute("currentPreset", preset);
        model.addAttribute("allPresets", DatePreset.values());

        if (activeWorkspace != null) {
            AnalyticsReadinessResponse readiness = datasetQueryService.getAnalyticsReadiness(activeWorkspace.getId());
            model.addAttribute("readiness", readiness);

            if (readiness.isSalesReady()) {
                CustomerIntelligenceResponse response = customerIntelligenceService.getCustomerIntelligence(activeWorkspace.getId(), filter);
                model.addAttribute("analytics", response);
            }

            // Phase 5 ML K-Means Clusters
            List<CustomerClusterAssignment> mlClusters = customerClusterAssignmentRepository.findByWorkspaceIdOrderByClusterIdAsc(activeWorkspace.getId());
            model.addAttribute("mlClusters", mlClusters);
        }

        return "analytics/customers";
    }

    @GetMapping("/api")
    @ResponseBody
    public CustomerIntelligenceResponse getCustomerApi(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      HttpSession session,
                                                      @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) return new CustomerIntelligenceResponse();

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        return customerIntelligenceService.getCustomerIntelligence(activeWorkspace.getId(), filter);
    }
}
