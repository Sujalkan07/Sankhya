package com.sankhya.analytics.ai.controller;

import com.sankhya.ai.gemini.GeminiChatService;
import com.sankhya.ai.gemini.GeminiInsightService;
import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.overview.dto.ExecutiveOverviewResponse;
import com.sankhya.analytics.overview.service.ExecutiveOverviewService;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.ml.entity.*;
import com.sankhya.ml.enums.AlgorithmType;
import com.sankhya.ml.enums.InsightCategory;
import com.sankhya.ml.execution.MLJobService;
import com.sankhya.ml.registry.ModelRegistryService;
import com.sankhya.ml.repository.*;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.workspace.dto.WorkspaceResponse;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/analytics/ai-insights")
public class AiInsightsController {

    private final WorkspaceService workspaceService;
    private final ExecutiveOverviewService executiveOverviewService;
    private final DatasetQueryService datasetQueryService;
    private final BusinessRecommendationRepository businessRecommendationRepository;
    private final MLJobService mlJobService;
    private final ModelRegistryService modelRegistryService;
    private final GeminiInsightService geminiInsightService;
    private final GeminiChatService geminiChatService;
    private final CustomerClusterAssignmentRepository customerClusterAssignmentRepository;
    private final AssociationRuleRepository associationRuleRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final DemandPredictionRepository demandPredictionRepository;

    public AiInsightsController(WorkspaceService workspaceService,
                                ExecutiveOverviewService executiveOverviewService,
                                DatasetQueryService datasetQueryService,
                                BusinessRecommendationRepository businessRecommendationRepository,
                                MLJobService mlJobService,
                                ModelRegistryService modelRegistryService,
                                GeminiInsightService geminiInsightService,
                                GeminiChatService geminiChatService,
                                CustomerClusterAssignmentRepository customerClusterAssignmentRepository,
                                AssociationRuleRepository associationRuleRepository,
                                ForecastResultRepository forecastResultRepository,
                                DemandPredictionRepository demandPredictionRepository) {
        this.workspaceService = workspaceService;
        this.executiveOverviewService = executiveOverviewService;
        this.datasetQueryService = datasetQueryService;
        this.businessRecommendationRepository = businessRecommendationRepository;
        this.mlJobService = mlJobService;
        this.modelRegistryService = modelRegistryService;
        this.geminiInsightService = geminiInsightService;
        this.geminiChatService = geminiChatService;
        this.customerClusterAssignmentRepository = customerClusterAssignmentRepository;
        this.associationRuleRepository = associationRuleRepository;
        this.forecastResultRepository = forecastResultRepository;
        this.demandPredictionRepository = demandPredictionRepository;
    }

    @GetMapping
    public String showAiInsights(@AuthenticationPrincipal CustomUserDetails userDetails,
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
        model.addAttribute("currentPage", "ai-insights");
        model.addAttribute("pageTitle", "AI Insights & Machine Learning Engine");

        if (activeWorkspace == null) {
            return "analytics/ai-insights";
        }

        Long wsId = activeWorkspace.getId();
        modelRegistryService.ensureDefaultModelsForWorkspace(wsId);

        AnalyticsReadinessResponse readiness = datasetQueryService.getAnalyticsReadiness(wsId);
        model.addAttribute("readiness", readiness);

        // Baseline telemetry for What-If simulator
        BigDecimal baselineRevenue = BigDecimal.valueOf(4865000);
        BigDecimal baselineMargin = BigDecimal.valueOf(18.3);
        BigDecimal baselineWorkingCapital = BigDecimal.valueOf(320000);

        if (readiness.isSalesReady()) {
            try {
                ExecutiveOverviewResponse overview = executiveOverviewService.getExecutiveOverview(
                        wsId, DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null));
                if (overview.getTotalRevenue() != null && overview.getTotalRevenue().getCurrentValue() instanceof BigDecimal revVal
                        && revVal.compareTo(BigDecimal.ZERO) > 0) {
                    baselineRevenue = revVal;
                }
                if (overview.getGrossMarginPercentage() != null
                        && overview.getGrossMarginPercentage().compareTo(BigDecimal.ZERO) > 0) {
                    baselineMargin = overview.getGrossMarginPercentage();
                }
            } catch (Exception ignored) {}
        }

        model.addAttribute("baselineRevenue", baselineRevenue);
        model.addAttribute("baselineMargin", baselineMargin);
        model.addAttribute("baselineWorkingCapital", baselineWorkingCapital);

        // Fetch active recommendations
        List<BusinessRecommendation> activeRecs = businessRecommendationRepository.findActiveByWorkspaceId(wsId);
        model.addAttribute("activeRecommendations", activeRecs);

        // Fetch ML Models & Latest Executions
        List<MLModel> models = modelRegistryService.getModelsForWorkspace(wsId);
        List<Map<String, Object>> mlCards = new ArrayList<>();

        for (MLModel m : models) {
            Map<String, Object> card = new HashMap<>();
            card.put("id", m.getId());
            card.put("name", m.getModelName());
            card.put("algorithm", m.getAlgorithm().name());
            card.put("algorithmDisplay", m.getAlgorithm().getDisplayName());
            card.put("type", m.getModelType().name());
            card.put("version", m.getVersion());
            card.put("status", m.getStatus().name());
            card.put("description", m.getDescription());

            Optional<MLModelExecution> latestExec = modelRegistryService.getLatestExecution(m.getId());
            if (latestExec.isPresent()) {
                MLModelExecution exec = latestExec.get();
                card.put("lastTrained", exec.getCompletedAt() != null ? exec.getCompletedAt() : exec.getStartedAt());
                card.put("durationMs", exec.getDurationMs());
                card.put("trainRecords", exec.getTrainRecordCount());
                card.put("metrics", modelRegistryService.parseMetrics(exec.getMetricsJson()));
            } else {
                card.put("lastTrained", "Not yet trained");
                card.put("durationMs", 0);
                card.put("trainRecords", 0);
                card.put("metrics", Collections.emptyMap());
            }
            mlCards.add(card);
        }
        model.addAttribute("mlCards", mlCards);

        // Fetch Gemini Insights
        Optional<GeminiInsightEntity> execSummary = geminiInsightService.getLatestInsight(wsId, InsightCategory.EXECUTIVE_SUMMARY);
        model.addAttribute("executiveSummary", execSummary.orElse(null));

        Optional<GeminiInsightEntity> salesInsight = geminiInsightService.getLatestInsight(wsId, InsightCategory.SALES_FORECAST);
        model.addAttribute("salesInsight", salesInsight.orElse(null));

        Optional<GeminiInsightEntity> customerInsight = geminiInsightService.getLatestInsight(wsId, InsightCategory.CUSTOMER_CLUSTERS);
        model.addAttribute("customerInsight", customerInsight.orElse(null));

        Optional<GeminiInsightEntity> affinityInsight = geminiInsightService.getLatestInsight(wsId, InsightCategory.PRODUCT_AFFINITY);
        model.addAttribute("affinityInsight", affinityInsight.orElse(null));

        Optional<GeminiInsightEntity> demandInsight = geminiInsightService.getLatestInsight(wsId, InsightCategory.DEMAND_RISK);
        model.addAttribute("demandInsight", demandInsight.orElse(null));

        // ML Output Previews
        List<AssociationRuleEntity> topRules = associationRuleRepository.findTop20ByWorkspaceIdOrderByLiftDesc(wsId);
        model.addAttribute("topAssociationRules", topRules.stream().limit(8).toList());

        List<ForecastResultEntity> forecasts = forecastResultRepository.findByWorkspaceIdOrderByForecastDateAsc(wsId);
        model.addAttribute("forecastResults", forecasts.stream().limit(14).toList());

        List<DemandPredictionEntity> demandRiskList = demandPredictionRepository.findByWorkspaceIdOrderByPredictedUnitsNext7dDesc(wsId);
        model.addAttribute("topDemandPredictions", demandRiskList.stream().limit(8).toList());

        List<CustomerClusterAssignment> clusterAssignments = customerClusterAssignmentRepository.findByWorkspaceIdOrderByClusterIdAsc(wsId);
        model.addAttribute("hasClusters", !clusterAssignments.isEmpty());

        model.addAttribute("activeJobStatus", mlJobService.getJobStatus(wsId));

        return "analytics/ai-insights";
    }

    @PostMapping("/train/{algorithm}")
    public String triggerTraining(@PathVariable("algorithm") String algorithm,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "No active workspace selected.");
            return "redirect:/analytics/ai-insights";
        }

        Long wsId = activeWorkspace.getId();
        try {
            switch (algorithm.toUpperCase()) {
                case "K_MEANS" -> {
                    MLModelExecution exec = mlJobService.executeKMeans(wsId, 4);
                    redirectAttributes.addFlashAttribute("successMessage", "K-Means Customer Clustering executed successfully (" + exec.getTrainRecordCount() + " customers analyzed).");
                }
                case "APRIORI" -> {
                    MLModelExecution exec = mlJobService.executeApriori(wsId, 0.01, 0.20, 1.05);
                    redirectAttributes.addFlashAttribute("successMessage", "Apriori Market Basket Analysis executed successfully (" + exec.getTrainRecordCount() + " baskets mined).");
                }
                case "RANDOM_FOREST" -> {
                    MLModelExecution exec = mlJobService.executeRandomForest(wsId, 30);
                    redirectAttributes.addFlashAttribute("successMessage", "Random Forest Product Demand Classifier trained successfully (" + exec.getTrainRecordCount() + " SKUs trained).");
                }
                case "LINEAR_REGRESSION" -> {
                    MLModelExecution exec = mlJobService.executeLinearRegression(wsId, 14);
                    redirectAttributes.addFlashAttribute("successMessage", "Linear Regression Sales Forecaster executed successfully (14-day trajectory projected).");
                }
                case "ALL" -> {
                    mlJobService.trainAllModelsAsync(wsId);
                    redirectAttributes.addFlashAttribute("successMessage", "Asynchronous multi-model training job started for all ML models.");
                }
                default -> redirectAttributes.addFlashAttribute("errorMessage", "Unknown algorithm: " + algorithm);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Training failed: " + e.getMessage());
        }

        return "redirect:/analytics/ai-insights";
    }

    @PostMapping("/generate/{category}")
    public String triggerGeminiGeneration(@PathVariable("category") String category,
                                          @AuthenticationPrincipal CustomUserDetails userDetails,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "No active workspace selected.");
            return "redirect:/analytics/ai-insights";
        }

        Long wsId = activeWorkspace.getId();
        try {
            InsightCategory cat = InsightCategory.valueOf(category.toUpperCase());
            GeminiInsightEntity entity = geminiInsightService.generateAndPersistInsight(wsId, cat);
            redirectAttributes.addFlashAttribute("successMessage", "Generated Gemini Insight: " + entity.getTitle());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Gemini generation error: " + e.getMessage());
        }

        return "redirect:/analytics/ai-insights";
    }

    @PostMapping("/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chatWithGemini(@RequestBody Map<String, String> request,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails,
                                                             HttpSession session) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "No active workspace"));
        }

        String message = request.getOrDefault("message", "");
        GeminiChatService.ChatResponse resp = geminiChatService.chat(activeWorkspace.getId(), message);

        Map<String, Object> body = new HashMap<>();
        body.put("success", resp.success());
        body.put("reply", resp.reply());
        body.put("modelVersion", resp.modelVersion());
        body.put("error", resp.error());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getJobStatus(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                           HttpSession session) {
        Workspace activeWorkspace = workspaceService.getActiveWorkspace(session, userDetails.getId());
        if (activeWorkspace == null) {
            return ResponseEntity.ok(Map.of("status", "NO_WORKSPACE"));
        }
        return ResponseEntity.ok(Map.of("status", mlJobService.getJobStatus(activeWorkspace.getId())));
    }
}
