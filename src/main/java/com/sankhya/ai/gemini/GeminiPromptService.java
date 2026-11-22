package com.sankhya.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.overview.dto.ExecutiveOverviewResponse;
import com.sankhya.analytics.overview.service.ExecutiveOverviewService;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.repository.DatasetRepository;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.ml.entity.AssociationRuleEntity;
import com.sankhya.ml.entity.CustomerClusterAssignment;
import com.sankhya.ml.entity.DemandPredictionEntity;
import com.sankhya.ml.entity.ForecastResultEntity;
import com.sankhya.ml.repository.AssociationRuleRepository;
import com.sankhya.ml.repository.CustomerClusterAssignmentRepository;
import com.sankhya.ml.repository.DemandPredictionRepository;
import com.sankhya.ml.repository.ForecastResultRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GeminiPromptService {

    private final ExecutiveOverviewService executiveOverviewService;
    private final CustomerClusterAssignmentRepository customerClusterAssignmentRepository;
    private final AssociationRuleRepository associationRuleRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final DemandPredictionRepository demandPredictionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DatasetRepository datasetRepository;
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;
    private final ObjectMapper objectMapper;

    public GeminiPromptService(ExecutiveOverviewService executiveOverviewService,
                               CustomerClusterAssignmentRepository customerClusterAssignmentRepository,
                               AssociationRuleRepository associationRuleRepository,
                               ForecastResultRepository forecastResultRepository,
                               DemandPredictionRepository demandPredictionRepository,
                               WorkspaceRepository workspaceRepository,
                               DatasetRepository datasetRepository,
                               UnifiedSalesMartRepository unifiedSalesMartRepository,
                               ObjectMapper objectMapper) {
        this.executiveOverviewService = executiveOverviewService;
        this.customerClusterAssignmentRepository = customerClusterAssignmentRepository;
        this.associationRuleRepository = associationRuleRepository;
        this.forecastResultRepository = forecastResultRepository;
        this.demandPredictionRepository = demandPredictionRepository;
        this.workspaceRepository = workspaceRepository;
        this.datasetRepository = datasetRepository;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
        this.objectMapper = objectMapper;
    }

    public String getSystemInstruction() {
        return "You are Sankhya AI, an executive supermarket analytics and retail intelligence consultant. " +
                "You provide data-grounded strategic insights, merchandise optimizations, and actionable advice to supermarket owners and category managers.\n" +
                "GUIDELINES:\n" +
                "1. STRICT GROUNDING: Rely strictly on the provided verified metrics, ML clusters, forecasts, and association rules. Do not hallucinate or invent imaginary numbers, brands, or customer contact details (email/phone are not present unless provided).\n" +
                "2. DYNAMIC DATA PROVENANCE DISCLOSURE: Check `datasetProvenance`, `dataSource`, `dateRange`, and currency indicators from the workspace telemetry JSON. Accurately disclose whether the metrics come from uploaded operational POS files, synthetic demonstration data, or Instacart-derived test fixtures—never misrepresent synthetic data as live retail store data.\n" +
                "3. STRUCTURE: Format your output with markdown headers (## Executive Takeaway, ### Analytical Findings, ### Actionable Merchandising Recommendations).\n" +
                "4. ACCURACY: Always quote actual revenue numbers, percentages, lift metrics, and cluster attributes from the context.\n" +
                "5. TONE: Professional, concise, executive, and commercially savvy.";
    }

    @Transactional(readOnly = true)
    public Map<String, Object> gatherWorkspaceTelemetry(Long workspaceId) {
        Map<String, Object> telemetry = new LinkedHashMap<>();

        Workspace ws = workspaceRepository.findById(workspaceId).orElse(null);
        telemetry.put("workspaceName", ws != null ? ws.getName() : "Supermarket");
        telemetry.put("currency", "INR");

        // 0. Dataset Provenance Metadata (stored dynamically per workspace)
        List<Dataset> datasets = datasetRepository.findByWorkspaceId(workspaceId);
        long martCount = unifiedSalesMartRepository.countByWorkspaceId(workspaceId);
        telemetry.put("dataSource", martCount > 0 ? "UNIFIED_SALES_MART" : "NORMALIZED_TABLES");
        telemetry.put("totalMartRecords", martCount);

        List<String> provenanceNotes = new ArrayList<>();
        for (Dataset d : datasets) {
            if (d.getRecordCount() != null && d.getRecordCount() > 0) {
                provenanceNotes.add(d.getName() + (d.getDescription() != null && !d.getDescription().isBlank()
                        ? " (" + d.getDescription() + ")" : ""));
            }
        }
        telemetry.put("datasetProvenance", provenanceNotes.isEmpty()
                ? "No active datasets loaded in workspace"
                : String.join("; ", provenanceNotes));

        if (martCount > 0) {
            LocalDateTime minDate = unifiedSalesMartRepository.findMinTransactionDateByWorkspaceId(workspaceId);
            LocalDateTime maxDate = unifiedSalesMartRepository.findMaxTransactionDateByWorkspaceId(workspaceId);
            telemetry.put("minTransactionDate", minDate != null ? minDate.toString() : "N/A");
            telemetry.put("maxTransactionDate", maxDate != null ? maxDate.toString() : "N/A");
        }

        // 1. Overview KPIs (Last 30 Days, fallback to This Year if 30d window empty)
        try {
            ExecutiveOverviewResponse overview = executiveOverviewService.getExecutiveOverview(
                    workspaceId, DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null));
            String windowLabel = "LAST_30_DAYS";
            if (overview != null && overview.getTotalRevenue() != null
                    && overview.getTotalRevenue().getCurrentValue() instanceof java.math.BigDecimal rev30
                    && rev30.compareTo(java.math.BigDecimal.ZERO) <= 0 && martCount > 0) {
                overview = executiveOverviewService.getExecutiveOverview(
                        workspaceId, DateRangeFilter.of(DatePreset.THIS_YEAR, null, null));
                windowLabel = "THIS_YEAR";
            }
            if (overview != null) {
                telemetry.put("kpiWindow", windowLabel);
                telemetry.put("totalRevenue", overview.getTotalRevenue() != null ? overview.getTotalRevenue().getCurrentValue() : 0);
                telemetry.put("totalTransactions", overview.getTotalTransactions() != null ? overview.getTotalTransactions().getCurrentValue() : 0);
                telemetry.put("averageTransactionValue", overview.getAverageTransactionValue() != null ? overview.getAverageTransactionValue().getCurrentValue() : 0);
                telemetry.put("grossMarginPercentage", overview.getGrossMarginPercentage() != null ? overview.getGrossMarginPercentage() : 0);
            }
        } catch (Exception ignored) {}

        // 2. Customer Cluster Summaries
        try {
            List<Map<String, Object>> clusters = customerClusterAssignmentRepository.findByWorkspaceIdOrderByClusterIdAsc(workspaceId)
                    .stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            CustomerClusterAssignment::getClusterLabel,
                            java.util.stream.Collectors.counting()
                    ))
                    .entrySet().stream()
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("label", e.getKey());
                        m.put("memberCount", e.getValue());
                        return m;
                    })
                    .toList();
            telemetry.put("customerClusters", clusters);
        } catch (Exception ignored) {}

        // 3. Top Association Rules
        try {
            List<AssociationRuleEntity> rules = associationRuleRepository.findTop20ByWorkspaceIdOrderByLiftDesc(workspaceId);
            List<Map<String, Object>> topRules = rules.stream().limit(5).map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("antecedent", r.getAntecedent());
                m.put("consequent", r.getConsequent());
                m.put("lift", String.format("%.2f", r.getLift()));
                m.put("confidence", String.format("%.1f%%", r.getConfidence() * 100));
                return m;
            }).toList();
            telemetry.put("topMarketBasketRules", topRules);
        } catch (Exception ignored) {}

        // 4. Revenue Forecast Summary
        try {
            List<ForecastResultEntity> forecasts = forecastResultRepository.findByWorkspaceIdOrderByForecastDateAsc(workspaceId);
            if (!forecasts.isEmpty()) {
                double totalPred = forecasts.stream().mapToDouble(ForecastResultEntity::getPredictedRevenue).sum();
                double baseline = forecasts.get(0).getBaselineRevenue() != null ? forecasts.get(0).getBaselineRevenue() * forecasts.size() : 0.0;
                telemetry.put("forecastDays", forecasts.size());
                telemetry.put("projectedRevenueNextPeriod", totalPred);
                telemetry.put("baselineRevenueNextPeriod", baseline);
                telemetry.put("projectedLiftVsBaseline", baseline > 0 ? ((totalPred - baseline) / baseline) * 100 : 0.0);
            }
        } catch (Exception ignored) {}

        // 5. High Risk Demand Predictions
        try {
            List<DemandPredictionEntity> demand = demandPredictionRepository.findByWorkspaceIdOrderByPredictedUnitsNext7dDesc(workspaceId);
            List<Map<String, Object>> highRisk = demand.stream()
                    .filter(d -> d.getStockoutRiskLevel() != null && d.getStockoutRiskLevel().name().contains("HIGH"))
                    .limit(5)
                    .map(d -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("productName", d.getProductName());
                        m.put("category", d.getCategory());
                        m.put("demandClass", d.getPredictedDemandClass().name());
                        m.put("projected7dUnits", d.getPredictedUnitsNext7d());
                        return m;
                    }).toList();
            telemetry.put("criticalStockoutRiskProducts", highRisk);
        } catch (Exception ignored) {}

        return telemetry;
    }

    public String buildPromptForCategory(Long workspaceId, String promptType) {
        Map<String, Object> telemetry = gatherWorkspaceTelemetry(workspaceId);
        String jsonContext;
        try {
            jsonContext = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(telemetry);
        } catch (Exception e) {
            jsonContext = telemetry.toString();
        }

        return switch (promptType.toUpperCase()) {
            case "EXECUTIVE_SUMMARY" ->
                    "Analyze the supermarket telemetry below and produce an authoritative Executive Business Summary covering overall financial health, high-impact growth levers, and critical risk areas.\n\n"
                    + "SUPERMARKET TELEMETRY CONTEXT:\n```json\n" + jsonContext + "\n```";

            case "SALES_FORECAST" ->
                    "Analyze the forward revenue forecast and baseline performance metrics from the context below. Detail expected sales trajectories, weekend peaks, and revenue preservation steps.\n\n"
                    + "SUPERMARKET TELEMETRY CONTEXT:\n```json\n" + jsonContext + "\n```";

            case "CUSTOMER_CLUSTERS" ->
                    "Examine the K-Means customer segmentation distribution in the context below. Explain behavioral differences across the clusters and recommend specific retention and basket-building strategies for each cohort.\n\n"
                    + "SUPERMARKET TELEMETRY CONTEXT:\n```json\n" + jsonContext + "\n```";

            case "PRODUCT_AFFINITY" ->
                    "Review the Apriori market basket association rules below. Propose concrete cross-merchandising displays, end-cap placements, and promo bundles to maximize cross-sell lift.\n\n"
                    + "SUPERMARKET TELEMETRY CONTEXT:\n```json\n" + jsonContext + "\n```";

            case "DEMAND_RISK" ->
                    "Examine the product demand predictions and stockout risk items in the context below. Recommend urgent safety-stock adjustments, supplier purchase orders, and inventory reallocations.\n\n"
                    + "SUPERMARKET TELEMETRY CONTEXT:\n```json\n" + jsonContext + "\n```";

            default ->
                    "Provide an actionable business intelligence assessment based on the verified supermarket telemetry below:\n\n"
                    + "SUPERMARKET TELEMETRY CONTEXT:\n```json\n" + jsonContext + "\n```";
        };
    }

    public String buildChatPrompt(Long workspaceId, String userMessage) {
        Map<String, Object> telemetry = gatherWorkspaceTelemetry(workspaceId);
        String jsonContext;
        try {
            jsonContext = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(telemetry);
        } catch (Exception e) {
            jsonContext = telemetry.toString();
        }

        return "User Question: \"" + userMessage + "\"\n\n"
                + "Please answer the user's question directly and concisely, referencing verified telemetry data where applicable.\n\n"
                + "VERIFIED SUPERMARKET TELEMETRY:\n```json\n" + jsonContext + "\n```";
    }
}
