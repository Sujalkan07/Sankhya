package com.sankhya.analytics.reports.controller;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.reports.service.AnalyticalReportingService;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/analytics/reports")
public class AnalyticalReportingController {

    private final WorkspaceService workspaceService;
    private final AnalyticalReportingService analyticalReportingService;
    private final DatasetQueryService datasetQueryService;

    public AnalyticalReportingController(WorkspaceService workspaceService,
                                         AnalyticalReportingService analyticalReportingService,
                                         DatasetQueryService datasetQueryService) {
        this.workspaceService = workspaceService;
        this.analyticalReportingService = analyticalReportingService;
        this.datasetQueryService = datasetQueryService;
    }

    @GetMapping
    public String showReportsCenter(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("activeModule", "reports");
        model.addAttribute("activeSubPage", "overview");
        model.addAttribute("currentPage", "reports");

        if (activeWorkspace != null) {
            model.addAttribute("readiness", datasetQueryService.getAnalyticsReadiness(activeWorkspace.getId()));
        }

        return "analytics/reports";
    }

    @GetMapping("/sales")
    public void exportSalesReport(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpSession session,
                                  @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                  HttpServletResponse response) throws IOException {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No active workspace selected");
            return;
        }

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"sankhya_sales_report_" + preset.name().toLowerCase() + ".csv\"");

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            analyticalReportingService.exportSalesTransactionsCsv(activeWorkspace.getId(), filter, writer);
        }
    }

    @GetMapping("/products")
    public void exportProductsReport(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     HttpSession session,
                                     @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                     HttpServletResponse response) throws IOException {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No active workspace selected");
            return;
        }

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"sankhya_product_performance_" + preset.name().toLowerCase() + ".csv\"");

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            analyticalReportingService.exportProductPerformanceCsv(activeWorkspace.getId(), filter, writer);
        }
    }

    @GetMapping("/customers")
    public void exportCustomersReport(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      HttpSession session,
                                      @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                      HttpServletResponse response) throws IOException {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No active workspace selected");
            return;
        }

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"sankhya_customer_rfm_" + preset.name().toLowerCase() + ".csv\"");

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            analyticalReportingService.exportCustomerIntelligenceCsv(activeWorkspace.getId(), filter, writer);
        }
    }

    @GetMapping("/inventory")
    public void exportInventoryReport(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      HttpSession session,
                                      @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                      HttpServletResponse response) throws IOException {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No active workspace selected");
            return;
        }

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"sankhya_inventory_health_" + preset.name().toLowerCase() + ".csv\"");

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            analyticalReportingService.exportInventoryHealthCsv(activeWorkspace.getId(), filter, writer);
        }
    }

    @GetMapping("/recommendations")
    public void exportRecommendationsReport(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            HttpSession session,
                                            HttpServletResponse response) throws IOException {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No active workspace selected");
            return;
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"sankhya_recommendations.csv\"");

        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            analyticalReportingService.exportRecommendationsCsv(activeWorkspace.getId(), writer);
        }
    }
}
