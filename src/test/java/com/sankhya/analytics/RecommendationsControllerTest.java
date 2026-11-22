package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.recommendations.controller.RecommendationsController;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.enums.RecommendationCategory;
import com.sankhya.analytics.recommendations.enums.RecommendationSeverity;
import com.sankhya.analytics.recommendations.enums.RecommendationStatus;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.analytics.recommendations.service.BusinessRecommendationService;
import com.sankhya.dataset.service.DatasetQueryService;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationsController Unit Tests")
class RecommendationsControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private BusinessRecommendationService businessRecommendationService;

    @Mock
    private BusinessRecommendationRepository businessRecommendationRepository;

    @Mock
    private DatasetQueryService datasetQueryService;

    private RecommendationsController controller;
    private CustomUserDetails userDetails;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        controller = new RecommendationsController(
                workspaceService,
                businessRecommendationService,
                businessRecommendationRepository,
                datasetQueryService
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
    @DisplayName("showRecommendations populates model, readiness, and summary counters")
    void testShowRecommendations() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        when(workspaceService.getActiveWorkspace(eq(session), eq(1L))).thenReturn(workspace);
        when(workspaceService.getUserWorkspaces(eq(1L), eq(10L))).thenReturn(Collections.emptyList());

        AnalyticsReadinessResponse readiness = new AnalyticsReadinessResponse();
        readiness.setSalesReady(true);
        when(datasetQueryService.getAnalyticsReadiness(10L)).thenReturn(readiness);

        BusinessRecommendation rec = new BusinessRecommendation();
        rec.setId(1L);
        rec.setTitle("Replenish Fresh Milk");
        rec.setSeverity(RecommendationSeverity.CRITICAL);
        rec.setStatus(RecommendationStatus.NEW);

        when(businessRecommendationService.getRecommendations(eq(10L), any(), any()))
                .thenReturn(List.of(rec));

        when(businessRecommendationRepository.countByWorkspaceIdAndStatus(10L, RecommendationStatus.NEW)).thenReturn(3L);
        when(businessRecommendationRepository.countByWorkspaceIdAndStatus(10L, RecommendationStatus.REVIEWED)).thenReturn(1L);
        when(businessRecommendationRepository.countByWorkspaceIdAndStatus(10L, RecommendationStatus.RESOLVED)).thenReturn(2L);
        when(businessRecommendationRepository.countByWorkspaceIdAndStatus(10L, RecommendationStatus.DISMISSED)).thenReturn(0L);

        when(businessRecommendationRepository.findActiveByWorkspaceId(10L)).thenReturn(List.of(rec));

        String view = controller.showRecommendations(userDetails, session, null, null, model);

        assertEquals("analytics/recommendations", view);
        assertEquals("recommendations", model.getAttribute("currentPage"));
        assertEquals(4L, model.getAttribute("totalActive"));
        assertEquals(1L, model.getAttribute("totalCritical"));
        assertEquals(1L, model.getAttribute("totalReviewed"));
        assertEquals(2L, model.getAttribute("totalResolved"));
    }

    @Test
    @DisplayName("updateStatus transitions recommendation status and redirects")
    void testUpdateStatus() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(workspaceService.getActiveWorkspace(eq(session), eq(1L))).thenReturn(workspace);

        String view = controller.updateStatus(userDetails, session, 5L, RecommendationStatus.RESOLVED, redirectAttributes);

        assertEquals("redirect:/analytics/recommendations", view);
        verify(businessRecommendationService).updateStatus(10L, 5L, RecommendationStatus.RESOLVED);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    @DisplayName("generateRecommendations triggers engine scan and redirects with count")
    void testGenerateRecommendations() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(workspaceService.getActiveWorkspace(eq(session), eq(1L))).thenReturn(workspace);

        BusinessRecommendation rec = new BusinessRecommendation();
        when(businessRecommendationService.generateRecommendations(eq(10L), any()))
                .thenReturn(List.of(rec));

        String view = controller.generateRecommendations(userDetails, session, DatePreset.LAST_30_DAYS, null, null, redirectAttributes);

        assertEquals("redirect:/analytics/recommendations", view);
        verify(businessRecommendationService).generateRecommendations(eq(10L), any());
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }
}
