package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.sales.controller.SalesAnalyticsController;
import com.sankhya.analytics.sales.dto.SalesAnalyticsResponse;
import com.sankhya.analytics.sales.dto.SalesKpiResponse;
import com.sankhya.analytics.sales.service.SalesAnalyticsService;
import com.sankhya.dataset.service.DatasetQueryService;
import com.sankhya.sales.entity.SalesTransaction;
import com.sankhya.sales.repository.SalesTransactionRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesAnalyticsController Unit Tests")
class SalesAnalyticsControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private SalesAnalyticsService salesAnalyticsService;

    @Mock
    private DatasetQueryService datasetQueryService;

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private com.sankhya.ml.repository.ForecastResultRepository forecastResultRepository;

    private SalesAnalyticsController controller;
    private CustomUserDetails userDetails;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        controller = new SalesAnalyticsController(
                workspaceService,
                salesAnalyticsService,
                datasetQueryService,
                salesTransactionRepository,
                forecastResultRepository
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
    @DisplayName("showSalesAnalytics returns correct view and populates recent transactions for drill-down")
    void testShowSalesAnalytics() {
        MockHttpSession session = new MockHttpSession();
        Model model = new ConcurrentModel();

        when(workspaceService.getActiveWorkspace(eq(session), eq(1L))).thenReturn(workspace);
        when(workspaceService.getUserWorkspaces(eq(1L), eq(10L))).thenReturn(Collections.emptyList());

        AnalyticsReadinessResponse readiness = new AnalyticsReadinessResponse();
        readiness.setSalesReady(true);
        when(datasetQueryService.getAnalyticsReadiness(10L)).thenReturn(readiness);

        SalesAnalyticsResponse analyticsResponse = new SalesAnalyticsResponse();
        analyticsResponse.setKpis(new SalesKpiResponse());
        when(salesAnalyticsService.getSalesAnalytics(eq(10L), any())).thenReturn(analyticsResponse);

        SalesTransaction tx = new SalesTransaction();
        tx.setId(100L);
        tx.setTransactionId("TX-1001");
        tx.setTransactionDate(LocalDateTime.now());
        tx.setProductId("SKU-1");
        tx.setQuantity(2);
        tx.setUnitPrice(BigDecimal.valueOf(100));
        tx.setRevenue(BigDecimal.valueOf(200));

        when(salesTransactionRepository.findByWorkspaceId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        String view = controller.showSalesAnalytics(userDetails, session, DatePreset.LAST_30_DAYS, null, null, model);

        assertEquals("analytics/sales", view);
        assertEquals("sales-analytics", model.getAttribute("currentPage"));
        assertNotNull(model.getAttribute("analytics"));

        @SuppressWarnings("unchecked")
        List<SalesTransaction> recent = (List<SalesTransaction>) model.getAttribute("recentTransactions");
        assertNotNull(recent);
        assertEquals(1, recent.size());
        assertEquals("TX-1001", recent.get(0).getTransactionId());
    }

    @Test
    @DisplayName("getTransactionLineItems API returns basket items for drill-down")
    void testGetTransactionLineItems() {
        MockHttpSession session = new MockHttpSession();

        when(workspaceService.getActiveWorkspace(eq(session), eq(1L))).thenReturn(workspace);

        SalesTransaction item1 = new SalesTransaction();
        item1.setTransactionId("TX-2002");
        item1.setProductId("SKU-1");
        item1.setQuantity(1);

        SalesTransaction item2 = new SalesTransaction();
        item2.setTransactionId("TX-2002");
        item2.setProductId("SKU-2");
        item2.setQuantity(3);

        when(salesTransactionRepository.findByWorkspaceIdAndTransactionId(10L, "TX-2002"))
                .thenReturn(List.of(item1, item2));

        ResponseEntity<List<SalesTransaction>> response = controller.getTransactionLineItems(userDetails, session, "TX-2002");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("SKU-1", response.getBody().get(0).getProductId());
        assertEquals("SKU-2", response.getBody().get(1).getProductId());
    }
}
