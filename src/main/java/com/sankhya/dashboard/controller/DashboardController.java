package com.sankhya.dashboard.controller;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.overview.dto.ExecutiveOverviewResponse;
import com.sankhya.analytics.overview.service.ExecutiveOverviewService;
import com.sankhya.dataset.service.DatasetQueryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    private final WorkspaceService workspaceService;
    private final ExecutiveOverviewService executiveOverviewService;
    private final DatasetQueryService datasetQueryService;

    public DashboardController(WorkspaceService workspaceService,
                               ExecutiveOverviewService executiveOverviewService,
                               DatasetQueryService datasetQueryService) {
        this.workspaceService = workspaceService;
        this.executiveOverviewService = executiveOverviewService;
        this.datasetQueryService = datasetQueryService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String showOverviewDashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("activeModule", "overview");
        model.addAttribute("activeSubPage", "overview");
        model.addAttribute("currentPage", "overview");

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        model.addAttribute("filter", filter);
        model.addAttribute("currentPreset", preset);
        model.addAttribute("allPresets", DatePreset.values());

        if (activeWorkspace != null) {
            AnalyticsReadinessResponse readiness = datasetQueryService.getAnalyticsReadiness(activeWorkspace.getId());
            model.addAttribute("readiness", readiness);

            if (readiness.isSalesReady()) {
                ExecutiveOverviewResponse overview = executiveOverviewService.getExecutiveOverview(activeWorkspace.getId(), filter);
                model.addAttribute("overview", overview);
            }
        }

        return "dashboard/overview";
    }

    @GetMapping("/api/overview")
    @ResponseBody
    public ExecutiveOverviewResponse getOverviewJson(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                     HttpSession session,
                                                     @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) return new ExecutiveOverviewResponse();

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        return executiveOverviewService.getExecutiveOverview(activeWorkspace.getId(), filter);
    }
}
