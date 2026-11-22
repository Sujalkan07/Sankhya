package com.sankhya.analytics.customers.service;

import com.sankhya.analytics.common.dto.ChartDataResponse;
import com.sankhya.analytics.common.dto.ComparisonMetric;
import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import com.sankhya.analytics.customers.dto.*;
import com.sankhya.customers.entity.Customer;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerIntelligenceService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final CustomerRepository customerRepository;

    public CustomerIntelligenceService(SalesTransactionRepository salesTransactionRepository,
                                       CustomerRepository customerRepository) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerIntelligenceResponse getCustomerIntelligence(Long workspaceId, DateRangeFilter filter) {
        CustomerIntelligenceResponse response = new CustomerIntelligenceResponse();

        // 1. Current Period Customer Counts
        long totalCustomers = customerRepository.countByWorkspaceId(workspaceId);
        long currActive = salesTransactionRepository.countActiveCustomersBetween(
                workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        long currNew = customerRepository.countByWorkspaceIdAndRegistrationDateBetween(
                workspaceId, filter.getStartDateTime().toLocalDate(), filter.getEndDateTime().toLocalDate());

        List<String> returningIds = salesTransactionRepository.findReturningCustomerIdsBetween(
                workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        long currReturning = returningIds.size();

        BigDecimal currRevenue = salesTransactionRepository.sumRevenueByWorkspaceIdAndDateBetween(
                workspaceId, filter.getStartDateTime(), filter.getEndDateTime());
        if (currRevenue == null) currRevenue = BigDecimal.ZERO;

        BigDecimal repeatPurchaseRate = currActive > 0
                ? AnalyticsMathUtils.safePercentage(BigDecimal.valueOf(currReturning), BigDecimal.valueOf(currActive))
                : BigDecimal.ZERO;

        BigDecimal arpu = currActive > 0
                ? AnalyticsMathUtils.safeDivide(currRevenue, BigDecimal.valueOf(currActive))
                : BigDecimal.ZERO;

        // 2. Preceding Period Customer Counts
        DateRangeFilter prevFilter = filter.getPrecedingComparisonFilter();
        long prevActive = salesTransactionRepository.countActiveCustomersBetween(
                workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
        long prevNew = customerRepository.countByWorkspaceIdAndRegistrationDateBetween(
                workspaceId, prevFilter.getStartDateTime().toLocalDate(), prevFilter.getEndDateTime().toLocalDate());
        List<String> prevReturningIds = salesTransactionRepository.findReturningCustomerIdsBetween(
                workspaceId, prevFilter.getStartDateTime(), prevFilter.getEndDateTime());
        long prevReturning = prevReturningIds.size();

        // 3. Assemble KPIs
        CustomerKpiResponse kpis = new CustomerKpiResponse();
        kpis.setTotalCustomers(AnalyticsMathUtils.calculateComparison(
                BigDecimal.valueOf(totalCustomers), BigDecimal.valueOf(totalCustomers), String.valueOf(totalCustomers)));
        kpis.setActiveCustomers(AnalyticsMathUtils.calculateComparison(
                BigDecimal.valueOf(currActive), BigDecimal.valueOf(prevActive), String.valueOf(currActive)));
        kpis.setNewCustomers(AnalyticsMathUtils.calculateComparison(
                BigDecimal.valueOf(currNew), BigDecimal.valueOf(prevNew), String.valueOf(currNew)));
        kpis.setReturningCustomers(AnalyticsMathUtils.calculateComparison(
                BigDecimal.valueOf(currReturning), BigDecimal.valueOf(prevReturning), String.valueOf(currReturning)));
        kpis.setRepeatPurchaseRate(repeatPurchaseRate);
        kpis.setAverageRevenuePerUser(arpu);
        response.setKpis(kpis);

        // 4. Customer Name Map
        List<Customer> customerEntities = customerRepository.findByWorkspaceId(workspaceId);
        Map<String, String> customerNames = customerEntities.stream()
                .collect(Collectors.toMap(
                        Customer::getCustomerId,
                        c -> c.getCustomerName() != null ? c.getCustomerName() : c.getCustomerId(),
                        (existing, replacement) -> existing
                ));

        // 5. Raw RFM Aggregations from Transactions
        List<Object[]> rfmRows = salesTransactionRepository.findCustomerRfmMetricsBetween(
                workspaceId, filter.getStartDateTime(), filter.getEndDateTime());

        List<RfmCustomerDto> rfmList = new ArrayList<>();
        LocalDateTime endDateTime = filter.getEndDateTime();

        for (Object[] row : rfmRows) {
            String cId = (String) row[0];
            if (cId == null || cId.trim().isEmpty()) continue;

            LocalDateTime lastTxnDate = (LocalDateTime) row[1];
            long freq = ((Number) row[2]).longValue();
            BigDecimal monetary = (BigDecimal) row[3];
            long recencyDays = Math.max(0, Duration.between(lastTxnDate, endDateTime).toDays());

            String name = customerNames.getOrDefault(cId, cId);
            RfmCustomerDto dto = new RfmCustomerDto();
            dto.setCustomerId(cId);
            dto.setCustomerName(name);
            dto.setRecencyDays(recencyDays);
            dto.setFrequency(freq);
            dto.setMonetary(monetary);
            rfmList.add(dto);
        }

        // 6. Calculate RFM Quintile Scores & Assign Segments
        assignRfmScoresAndSegments(rfmList);
        response.setRfmCustomers(rfmList);

        // 7. Top Customers by Spend
        List<RfmCustomerDto> topSpend = rfmList.stream()
                .sorted(Comparator.comparing(RfmCustomerDto::getMonetary).reversed())
                .limit(10)
                .collect(Collectors.toList());
        response.setTopCustomers(topSpend);

        // 8. Segment Summaries & Chart
        Map<String, List<RfmCustomerDto>> grouped = rfmList.stream()
                .collect(Collectors.groupingBy(RfmCustomerDto::getSegment));

        List<String> canonicalSegments = List.of(
                "Champions", "Loyal Customers", "Potential Loyalists", "New Customers",
                "Promising", "Cannot Lose Them", "At Risk", "Hibernating", "Lost"
        );

        List<CustomerSegmentSummaryDto> summaries = new ArrayList<>();
        ChartDataResponse segChart = new ChartDataResponse();
        List<BigDecimal> segCounts = new ArrayList<>();

        for (String seg : canonicalSegments) {
            List<RfmCustomerDto> inSeg = grouped.getOrDefault(seg, Collections.emptyList());
            long count = inSeg.size();
            BigDecimal totalSpent = inSeg.stream()
                    .map(RfmCustomerDto::getMonetary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgSpent = count > 0
                    ? AnalyticsMathUtils.safeDivide(totalSpent, BigDecimal.valueOf(count))
                    : BigDecimal.ZERO;
            BigDecimal share = currActive > 0
                    ? AnalyticsMathUtils.safePercentage(BigDecimal.valueOf(count), BigDecimal.valueOf(currActive))
                    : BigDecimal.ZERO;

            summaries.add(new CustomerSegmentSummaryDto(seg, count, share, totalSpent, avgSpent));
            segChart.getLabels().add(seg);
            segCounts.add(BigDecimal.valueOf(count));
        }

        segChart.addDataset("Customers", segCounts);
        response.setSegmentSummaries(summaries);
        response.setSegmentChart(segChart);

        return response;
    }

    private void assignRfmScoresAndSegments(List<RfmCustomerDto> customers) {
        int n = customers.size();
        if (n == 0) return;

        if (n >= 5) {
            // Recency Score (lower days -> higher score 5 down to 1)
            customers.sort(Comparator.comparingLong(RfmCustomerDto::getRecencyDays));
            for (int i = 0; i < n; i++) {
                int score = 5 - (int) ((long) i * 5 / n);
                customers.get(i).setrScore(Math.max(1, Math.min(5, score)));
            }

            // Frequency Score (higher frequency -> higher score 5 down to 1)
            customers.sort(Comparator.comparingLong(RfmCustomerDto::getFrequency).reversed());
            for (int i = 0; i < n; i++) {
                int score = 5 - (int) ((long) i * 5 / n);
                customers.get(i).setfScore(Math.max(1, Math.min(5, score)));
            }

            // Monetary Score (higher monetary -> higher score 5 down to 1)
            customers.sort(Comparator.comparing(RfmCustomerDto::getMonetary).reversed());
            for (int i = 0; i < n; i++) {
                int score = 5 - (int) ((long) i * 5 / n);
                customers.get(i).setmScore(Math.max(1, Math.min(5, score)));
            }
        } else {
            // Deterministic Threshold fallback for small sample size
            for (RfmCustomerDto c : customers) {
                // R
                if (c.getRecencyDays() <= 7) c.setrScore(5);
                else if (c.getRecencyDays() <= 14) c.setrScore(4);
                else if (c.getRecencyDays() <= 30) c.setrScore(3);
                else if (c.getRecencyDays() <= 60) c.setrScore(2);
                else c.setrScore(1);

                // F
                if (c.getFrequency() >= 5) c.setfScore(5);
                else if (c.getFrequency() >= 4) c.setfScore(4);
                else if (c.getFrequency() >= 3) c.setfScore(3);
                else if (c.getFrequency() >= 2) c.setfScore(2);
                else c.setfScore(1);

                // M
                if (c.getMonetary().compareTo(BigDecimal.valueOf(500)) >= 0) c.setmScore(5);
                else if (c.getMonetary().compareTo(BigDecimal.valueOf(250)) >= 0) c.setmScore(4);
                else if (c.getMonetary().compareTo(BigDecimal.valueOf(100)) >= 0) c.setmScore(3);
                else if (c.getMonetary().compareTo(BigDecimal.valueOf(50)) >= 0) c.setmScore(2);
                else c.setmScore(1);
            }
        }

        // Assign 9 Segments
        for (RfmCustomerDto c : customers) {
            int r = c.getrScore();
            int f = c.getfScore();
            int m = c.getmScore();

            String segment;
            if (r >= 4 && f >= 4 && m >= 4) {
                segment = "Champions";
            } else if (r >= 3 && f >= 3 && m >= 3) {
                segment = "Loyal Customers";
            } else if (r >= 4 && f <= 2 && m >= 2) {
                segment = "Potential Loyalists";
            } else if (r >= 4 && f == 1) {
                segment = "New Customers";
            } else if (r <= 2 && f >= 4 && m >= 4) {
                segment = "Cannot Lose Them";
            } else if (r <= 2 && f >= 3 && m >= 3) {
                segment = "At Risk";
            } else if (r == 1 && f == 1 && m == 1) {
                segment = "Lost";
            } else if (r <= 2 && f <= 2 && m <= 2) {
                segment = "Hibernating";
            } else if (r >= 3) {
                segment = "Promising";
            } else {
                segment = "At Risk";
            }
            c.setSegment(segment);
        }
    }
}
