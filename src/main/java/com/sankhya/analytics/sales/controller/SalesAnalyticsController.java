package com.sankhya.analytics.sales.controller;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.sales.dto.SalesAnalyticsResponse;
import com.sankhya.analytics.sales.service.SalesAnalyticsService;
import com.sankhya.dataset.entity.UnifiedSalesMart;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.ml.entity.ForecastResultEntity;
import com.sankhya.ml.repository.ForecastResultRepository;
import com.sankhya.sales.entity.SalesTransaction;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/analytics/sales")
public class SalesAnalyticsController {

    private final WorkspaceService workspaceService;
    private final SalesAnalyticsService salesAnalyticsService;
    private final DatasetQueryService datasetQueryService;
    private final SalesTransactionRepository salesTransactionRepository;
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;
    private final ForecastResultRepository forecastResultRepository;

    public SalesAnalyticsController(WorkspaceService workspaceService,
                                    SalesAnalyticsService salesAnalyticsService,
                                    DatasetQueryService datasetQueryService,
                                    SalesTransactionRepository salesTransactionRepository,
                                    UnifiedSalesMartRepository unifiedSalesMartRepository,
                                    ForecastResultRepository forecastResultRepository) {
        this.workspaceService = workspaceService;
        this.salesAnalyticsService = salesAnalyticsService;
        this.datasetQueryService = datasetQueryService;
        this.salesTransactionRepository = salesTransactionRepository;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
        this.forecastResultRepository = forecastResultRepository;
    }

    @GetMapping
    public String showSalesAnalytics(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("activeModule", "sales");
        model.addAttribute("activeSubPage", "overview");
        model.addAttribute("currentPage", "sales-analytics");
        model.addAttribute("pageTitle", "Sales Intelligence & Basket Dynamics");

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        model.addAttribute("currentPreset", filter.getPreset());
        model.addAttribute("startDate", filter.getStartDate());
        model.addAttribute("endDate", filter.getEndDate());
        model.addAttribute("allPresets", DatePreset.values());

        if (activeWorkspace != null) {
            Long wsId = activeWorkspace.getId();
            AnalyticsReadinessResponse readiness = datasetQueryService.getAnalyticsReadiness(wsId);
            model.addAttribute("readiness", readiness);

            if (readiness.isSalesReady()) {
                SalesAnalyticsResponse response = salesAnalyticsService.getSalesAnalytics(wsId, filter);
                model.addAttribute("analytics", response);

                // Fetch recent transactions for drill-down inspection (consistent source-of-truth)
                List<SalesTransaction> recentTransactions;
                if (unifiedSalesMartRepository.countByWorkspaceId(wsId) > 0) {
                    recentTransactions = unifiedSalesMartRepository.findTransactionsInDateRange(
                            wsId,
                            filter.getStartDateTime(),
                            filter.getEndDateTime(),
                            PageRequest.of(0, 15)
                    ).getContent().stream().map(this::toViewTransaction).toList();
                } else {
                    recentTransactions = salesTransactionRepository.findByWorkspaceId(
                            wsId,
                            PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "transactionDate"))
                    ).getContent();
                }
                model.addAttribute("recentTransactions", recentTransactions);
            }

            // Phase 5 ML Forward Revenue Forecast
            List<ForecastResultEntity> forecastResults = forecastResultRepository.findByWorkspaceIdOrderByForecastDateAsc(wsId);
            model.addAttribute("forecastResults", forecastResults);
        }

        return "analytics/sales";
    }

    @GetMapping("/api")
    @ResponseBody
    public SalesAnalyticsResponse getSalesAnalyticsApi(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        HttpSession session,
                                                        @RequestParam(required = false, defaultValue = "LAST_30_DAYS") DatePreset preset,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) return null;

        DateRangeFilter filter = DateRangeFilter.of(preset, startDate, endDate);
        return salesAnalyticsService.getSalesAnalytics(activeWorkspace.getId(), filter);
    }

    @GetMapping("/api/transactions/{transactionId}")
    @ResponseBody
    public ResponseEntity<List<SalesTransaction>> getTransactionLineItems(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            @PathVariable String transactionId) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            return ResponseEntity.badRequest().build();
        }

        Long wsId = activeWorkspace.getId();
        List<SalesTransaction> items;
        if (unifiedSalesMartRepository.countByWorkspaceId(wsId) > 0) {
            items = unifiedSalesMartRepository.findByWorkspaceIdAndTransactionId(wsId, transactionId)
                    .stream().map(this::toViewTransaction).toList();
        } else {
            items = salesTransactionRepository.findByWorkspaceIdAndTransactionId(wsId, transactionId);
        }
        return ResponseEntity.ok(items);
    }

    private SalesTransaction toViewTransaction(UnifiedSalesMart m) {
        SalesTransaction tx = new SalesTransaction();
        tx.setTransactionId(m.getTransactionId());
        tx.setTransactionDate(m.getTransactionDate());
        tx.setCustomerId(m.getCustomerId());
        tx.setProductId(m.getProductId());
        tx.setQuantity(m.getQuantity());
        tx.setUnitPrice(m.getUnitPrice());
        tx.setDiscount(m.getDiscount());
        tx.setRevenue(m.getRevenue());
        tx.setPaymentMethod(m.getPaymentMethod());
        tx.setStoreId(m.getStoreId());
        tx.setCashierName(m.getCashierName());
        tx.setStatus("COMPLETED");
        return tx;
    }
}
