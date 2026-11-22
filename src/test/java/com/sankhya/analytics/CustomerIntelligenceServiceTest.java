package com.sankhya.analytics;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.customers.dto.CustomerIntelligenceResponse;
import com.sankhya.analytics.customers.service.CustomerIntelligenceService;
import com.sankhya.customers.entity.Customer;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerIntelligenceService Unit Tests")
class CustomerIntelligenceServiceTest {

    @Mock
    private SalesTransactionRepository salesTransactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    private CustomerIntelligenceService customerIntelligenceService;

    @BeforeEach
    void setUp() {
        customerIntelligenceService = new CustomerIntelligenceService(salesTransactionRepository, customerRepository);
    }

    @Test
    @DisplayName("getCustomerIntelligence calculates KPIs, RFM quintiles and 9 segments correctly")
    void testGetCustomerIntelligenceSuccess() {
        Long workspaceId = 1L;
        DateRangeFilter filter = DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null);

        when(customerRepository.countByWorkspaceId(workspaceId)).thenReturn(50L);
        when(salesTransactionRepository.countActiveCustomersBetween(eq(workspaceId), any(), any()))
                .thenReturn(10L, 8L); // curr, prev
        when(customerRepository.countByWorkspaceIdAndRegistrationDateBetween(eq(workspaceId), any(), any()))
                .thenReturn(3L, 2L); // curr, prev
        when(salesTransactionRepository.findReturningCustomerIdsBetween(eq(workspaceId), any(), any()))
                .thenReturn(List.of("CUST-1", "CUST-2", "CUST-3")); // 3 returning
        when(salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(eq(workspaceId), any(), any()))
                .thenReturn(new BigDecimal("5000.00"));

        when(customerRepository.findByWorkspaceId(workspaceId)).thenReturn(Collections.emptyList());

        // Mock 5 customers for RFM quintiles
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> rfmRows = List.<Object[]>of(
                new Object[]{"CUST-1", now.minusDays(1), 12L, new BigDecimal("1200.00")}, // High R, High F, High M -> Champion
                new Object[]{"CUST-2", now.minusDays(2), 8L, new BigDecimal("800.00")},   // Loyal
                new Object[]{"CUST-3", now.minusDays(5), 4L, new BigDecimal("300.00")},
                new Object[]{"CUST-4", now.minusDays(20), 2L, new BigDecimal("150.00")},
                new Object[]{"CUST-5", now.minusDays(45), 1L, new BigDecimal("40.00")}    // Low R, Low F, Low M -> Lost / Hibernating
        );
        when(salesTransactionRepository.findCustomerRfmMetricsBetween(eq(workspaceId), any(), any()))
                .thenReturn(rfmRows);

        CustomerIntelligenceResponse response = customerIntelligenceService.getCustomerIntelligence(workspaceId, filter);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(50), response.getKpis().getTotalCustomers().getCurrentValue());
        assertEquals(BigDecimal.valueOf(10), response.getKpis().getActiveCustomers().getCurrentValue());
        assertEquals(BigDecimal.valueOf(3), response.getKpis().getReturningCustomers().getCurrentValue());
        assertEquals(new BigDecimal("30.00"), response.getKpis().getRepeatPurchaseRate()); // 3/10 * 100 = 30.00%
        assertEquals(new BigDecimal("500.00"), response.getKpis().getAverageRevenuePerUser()); // 5000 / 10 = 500.00

        // RFM scoring
        assertEquals(5, response.getRfmCustomers().size());
        assertEquals("CUST-1", response.getRfmCustomers().get(0).getCustomerId());
        assertEquals(5, response.getRfmCustomers().get(0).getrScore());
        assertEquals(5, response.getRfmCustomers().get(0).getfScore());
        assertEquals(5, response.getRfmCustomers().get(0).getmScore());
        assertEquals("Champions", response.getRfmCustomers().get(0).getSegment());

        // Check canonical segment summaries
        assertEquals(9, response.getSegmentSummaries().size());
    }
}
