package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.products.dto.ProductIntelligenceResponse;
import com.sankhya.analytics.products.service.ProductIntelligenceService;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductIntelligenceService Unit Tests")
class ProductIntelligenceServiceTest {

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private ProductRepository productRepository;

    private ProductIntelligenceService productIntelligenceService;

    @BeforeEach
    void setUp() {
        productIntelligenceService = new ProductIntelligenceService(salesTransactionRepository, productRepository);
    }

    @Test
    @DisplayName("getProductIntelligence calculates gross margins and BCG performance quadrants")
    void testGetProductIntelligenceSuccess() {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);

        when(productRepository.countByWorkspaceId(workspaceId)).thenReturn(20L);

        // Mock 4 products for 4 quadrants:
        // Product A: Vol 100, Margin 50% -> Star
        // Product B: Vol 120, Margin 10% -> Cash Cow
        // Product C: Vol 10, Margin 60%  -> Opportunity
        // Product D: Vol 5, Margin 5%    -> Underperformer
        List<Object[]> rawProds = List.<Object[]>of(
                new Object[]{"PROD-A", "Premium Organic Ghee", "Dairy", 100L, new BigDecimal("2000.00"), new BigDecimal("1000.00")}, // 50% margin
                new Object[]{"PROD-B", "Standard Milk 500ml", "Dairy", 120L, new BigDecimal("1200.00"), new BigDecimal("1080.00")}, // 10% margin
                new Object[]{"PROD-C", "Imported Swiss Cheese", "Dairy", 10L, new BigDecimal("500.00"), new BigDecimal("200.00")},    // 60% margin
                new Object[]{"PROD-D", "Discount Biscuits", "Snacks", 5L, new BigDecimal("50.00"), new BigDecimal("47.50")}          // 5% margin
        );
        when(salesTransactionRepository.findProductPerformanceBetween(eq(workspaceId), any(), any()))
                .thenReturn(rawProds);

        when(salesTransactionRepository.findCategorySalesContributionBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"Dairy", new BigDecimal("3700.00"), 230L, 150L, new BigDecimal("2280.00")},
                        new Object[]{"Snacks", new BigDecimal("50.00"), 5L, 5L, new BigDecimal("47.50")}
                ));

        when(productRepository.findCategoryProductSummaryByWorkspaceId(workspaceId))
                .thenReturn(List.<Object[]>of(
                        new Object[]{"Dairy", 15L, new BigDecimal("100.00"), new BigDecimal("60.00")},
                        new Object[]{"Snacks", 5L, new BigDecimal("20.00"), new BigDecimal("15.00")}
                ));

        ProductIntelligenceResponse response = productIntelligenceService.getProductIntelligence(workspaceId, filter);

        assertNotNull(response);
        assertEquals(20L, response.getKpis().getTotalCatalogProducts());
        assertEquals(4L, response.getKpis().getActiveSellingProducts());
        assertEquals(235L, response.getKpis().getTotalUnitsSold());
        assertEquals("Dairy", response.getKpis().getTopPerformingCategory());

        // Check quadrants
        assertTrue(response.getStarCount() >= 1);
        assertTrue(response.getCashCowCount() >= 1);
        assertTrue(response.getOpportunityCount() >= 1);
        assertTrue(response.getUnderperformerCount() >= 1);

        // Check category profitability
        assertEquals(2, response.getCategoryProfitability().size());
        assertEquals("Dairy", response.getCategoryProfitability().get(0).getCategory());
        assertEquals(new BigDecimal("1420.00"), response.getCategoryProfitability().get(0).getGrossProfit());
    }
}
