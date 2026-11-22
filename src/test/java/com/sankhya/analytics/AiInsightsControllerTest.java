package com.sankhya.analytics;

import com.sankhya.ai.gemini.GeminiChatService;
import com.sankhya.ai.gemini.GeminiInsightService;
import com.sankhya.analytics.ai.controller.AiInsightsController;
import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.dto.ComparisonMetric;
import com.sankhya.analytics.overview.dto.ExecutiveOverviewResponse;
import com.sankhya.analytics.overview.service.ExecutiveOverviewService;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.ml.execution.MLJobService;
import com.sankhya.ml.registry.ModelRegistryService;
import com.sankhya.ml.repository.AssociationRuleRepository;
import com.sankhya.ml.repository.CustomerClusterAssignmentRepository;
import com.sankhya.ml.repository.DemandPredictionRepository;
import com.sankhya.ml.repository.ForecastResultRepository;
import com.sankhya.security.CustomUserDetails;
import com.sankhya.user.entity.User;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiInsightsController Unit Tests")
class AiInsightsControllerTest {

    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private ExecutiveOverviewService executiveOverviewService;
    @Mock
    private com.sankhya.analytics.inventory.service.InventoryIntelligenceService inventoryIntelligenceService;
    @Mock
    private DatasetQueryService datasetQueryService;
    @Mock
    private BusinessRecommendationRepository businessRecommendationRepository;
    @Mock
    private MLJobService mlJobService;
    @Mock
    private ModelRegistryService modelRegistryService;
    @Mock
    private GeminiInsightService geminiInsightService;
    @Mock
    private GeminiChatService geminiChatService;
    @Mock
    private CustomerClusterAssignmentRepository customerClusterAssignmentRepository;
    @Mock
    private AssociationRuleRepository associationRuleRepository;
    @Mock
    private ForecastResultRepository forecastResultRepository;
    @Mock
    private DemandPredictionRepository demandPredictionRepository;

    private AiInsightsController controller;
    private CustomUserDetails userDetails;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        controller = new AiInsightsController(
                workspaceService,
                executiveOverviewService,
                inventoryIntelligenceService,
                datasetQueryService,
                businessRecommendationRepository,
                mlJobService,
                modelRegistryService,
                geminiInsightService,
                geminiChatService,
                customerClusterAssignmentRepository,
                associationRuleRepository,
                forecastResultRepository,
                demandPredictionRepository
        );

        User user = new User();
        user.setId(1L);
        user.setEmail("owner@supermarket.com");
        user.setFullName("Supermarket Owner");
        userDetails = new CustomUserDetails(user);

        workspace = new Workspace();
        workspace.setId(10L);
        workspace.setName("Green Basket Hub");
    }

    @Test
    @DisplayName("showAiInsights returns correct view and populates What-If baseline & ML cards")
    void testShowAiInsightsPopulatesModel() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        when(workspaceService.getActiveWorkspace(eq(session), eq(1L))).thenReturn(workspace);
        when(workspaceService.getUserWorkspaces(eq(1L), eq(10L))).thenReturn(Collections.emptyList());

        AnalyticsReadinessResponse readiness = new AnalyticsReadinessResponse();
        readiness.setSalesRecordCount(500);
        readiness.setSalesReady(true);
        when(datasetQueryService.getAnalyticsReadiness(10L)).thenReturn(readiness);

        ExecutiveOverviewResponse overview = new ExecutiveOverviewResponse();
        overview.setTotalRevenue(new ComparisonMetric<>(BigDecimal.valueOf(5000000), BigDecimal.valueOf(4500000), "₹50,00,000"));
        overview.setGrossMarginPercentage(BigDecimal.valueOf(19.5));
        when(executiveOverviewService.getExecutiveOverview(eq(10L), any())).thenReturn(overview);

        when(businessRecommendationRepository.findActiveByWorkspaceId(10L)).thenReturn(Collections.emptyList());
        when(modelRegistryService.getModelsForWorkspace(10L)).thenReturn(Collections.emptyList());
        when(mlJobService.getJobStatus(10L)).thenReturn("IDLE");

        String view = controller.showAiInsights(userDetails, session, model);

        assertEquals("analytics/ai-insights", view);
        assertEquals("ai-insights", model.getAttribute("currentPage"));
        assertEquals("Ask Naveen Store AI", model.getAttribute("pageTitle"));
        assertEquals(BigDecimal.valueOf(5000000), model.getAttribute("baselineRevenue"));
        assertEquals(BigDecimal.valueOf(19.5), model.getAttribute("baselineMargin"));
        assertEquals(true, model.getAttribute("simulatorReady"));
        assertNotNull(model.getAttribute("mlCards"));
    }

    @Test
    @DisplayName("chatWithGemini returns structured Ask Naveen Store AI payload")
    void testChatWithGemini() {
        MockHttpSession session = new MockHttpSession();
        when(workspaceService.getActiveWorkspace(eq(session), eq(1L))).thenReturn(workspace);

        when(geminiChatService.chatWithContext(eq(10L), eq("Hello"), any()))
                .thenReturn(new GeminiChatService.StructuredChatResponse(
                        true,
                        "Store revenue is ₹18,25,42,617.45.",
                        "gemini-3.5-flash-lite",
                        "STORE_OVERVIEW",
                        "2025-11-22 to 2026-11-22",
                        Collections.emptyList(),
                        null,
                        null,
                        Map.of("storeKey", "STORE-001", "currency", "INR (₹)"),
                        null
                ));

        ResponseEntity<Map<String, Object>> response = controller.chatWithGemini(
                Map.of("message", "Hello"), userDetails, session);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("Store revenue is ₹18,25,42,617.45.", response.getBody().get("reply"));
        assertEquals("STORE_OVERVIEW", response.getBody().get("intent"));
    }

    @Test
    @DisplayName("triggerTraining initiates ML job and redirects")
    void testTriggerTraining() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes ra = new RedirectAttributesModelMap();
        when(workspaceService.getActiveWorkspace(eq(session), eq(1L))).thenReturn(workspace);

        String view = controller.triggerTraining("ALL", userDetails, session, ra);
        assertEquals("redirect:/analytics/ai-insights", view);
        assertTrue(ra.getFlashAttributes().containsKey("successMessage"));
    }
}
