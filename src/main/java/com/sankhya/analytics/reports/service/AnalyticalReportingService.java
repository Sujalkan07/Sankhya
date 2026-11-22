package com.sankhya.analytics.reports.service;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.util.AnalyticsMathUtils;
import com.sankhya.analytics.customers.dto.CustomerIntelligenceResponse;
import com.sankhya.analytics.customers.dto.RfmCustomerDto;
import com.sankhya.analytics.customers.service.CustomerIntelligenceService;
import com.sankhya.analytics.inventory.dto.InventoryIntelligenceResponse;
import com.sankhya.analytics.inventory.dto.ReorderRecommendationDto;
import com.sankhya.analytics.inventory.service.InventoryIntelligenceService;
import com.sankhya.analytics.products.dto.ProductIntelligenceResponse;
import com.sankhya.analytics.products.dto.ProductPerformanceDto;
import com.sankhya.analytics.products.service.ProductIntelligenceService;
import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.service.BusinessRecommendationService;
import com.sankhya.sales.entity.SalesTransaction;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Service
public class AnalyticalReportingService {

    private final SalesTransactionRepository salesTransactionRepository;
    private final ProductIntelligenceService productIntelligenceService;
    private final CustomerIntelligenceService customerIntelligenceService;
    private final InventoryIntelligenceService inventoryIntelligenceService;
    private final BusinessRecommendationService businessRecommendationService;

    public AnalyticalReportingService(SalesTransactionRepository salesTransactionRepository,
                                      ProductIntelligenceService productIntelligenceService,
                                      CustomerIntelligenceService customerIntelligenceService,
                                      InventoryIntelligenceService inventoryIntelligenceService,
                                      BusinessRecommendationService businessRecommendationService) {
        this.salesTransactionRepository = salesTransactionRepository;
        this.productIntelligenceService = productIntelligenceService;
        this.customerIntelligenceService = customerIntelligenceService;
        this.inventoryIntelligenceService = inventoryIntelligenceService;
        this.businessRecommendationService = businessRecommendationService;
    }

    @Transactional(readOnly = true)
    public void exportSalesTransactionsCsv(Long workspaceId, DateRangeFilter filter, Writer writer) throws IOException {
        writer.write("Transaction ID,Date,Product ID,Quantity,Unit Price,Discount,Revenue,Payment Method,Customer ID,Status\n");

        int pageNumber = 0;
        int pageSize = 1000;
        Page<SalesTransaction> page;

        do {
            page = salesTransactionRepository.findTransactionsInDateRange(
                    workspaceId, filter.getStartDateTime(), filter.getEndDateTime(), PageRequest.of(pageNumber, pageSize));

            for (SalesTransaction t : page.getContent()) {
                writer.write(String.format("%s,%s,%s,%d,%s,%s,%s,%s,%s,%s\n",
                        escapeCsv(t.getTransactionId()),
                        t.getTransactionDate() != null ? t.getTransactionDate().toString() : "",
                        escapeCsv(t.getProductId()),
                        t.getQuantity() != null ? t.getQuantity() : 0,
                        t.getUnitPrice() != null ? t.getUnitPrice().toString() : "0.00",
                        t.getDiscount() != null ? t.getDiscount().toString() : "0.00",
                        t.getRevenue() != null ? t.getRevenue().toString() : "0.00",
                        escapeCsv(t.getPaymentMethod()),
                        escapeCsv(t.getCustomerId()),
                        escapeCsv(t.getStatus())
                ));
            }
            writer.flush();
            pageNumber++;
        } while (page.hasNext());
    }

    @Transactional(readOnly = true)
    public void exportProductPerformanceCsv(Long workspaceId, DateRangeFilter filter, Writer writer) throws IOException {
        writer.write("Product ID,Product Name,Category,Units Sold,Total Revenue,Estimated COGS,Gross Profit,Gross Margin %,Performance Quadrant\n");

        ProductIntelligenceResponse response = productIntelligenceService.getProductIntelligence(workspaceId, filter);
        for (ProductPerformanceDto p : response.getAllProducts()) {
            writer.write(String.format("%s,%s,%s,%d,%s,%s,%s,%s%%,%s\n",
                    escapeCsv(p.getProductId()),
                    escapeCsv(p.getProductName()),
                    escapeCsv(p.getCategory()),
                    p.getQuantitySold(),
                    AnalyticsMathUtils.roundMoney(p.getRevenue()),
                    AnalyticsMathUtils.roundMoney(p.getCogs()),
                    AnalyticsMathUtils.roundMoney(p.getGrossProfit()),
                    p.getGrossMarginPercentage(),
                    escapeCsv(p.getQuadrant())
            ));
        }
        writer.flush();
    }

    @Transactional(readOnly = true)
    public void exportCustomerIntelligenceCsv(Long workspaceId, DateRangeFilter filter, Writer writer) throws IOException {
        writer.write("Customer ID,Customer Name,Recency (Days),Frequency,Monetary Spend ($),R Score,F Score,M Score,RFM Code,Assigned Segment\n");

        CustomerIntelligenceResponse response = customerIntelligenceService.getCustomerIntelligence(workspaceId, filter);
        for (RfmCustomerDto c : response.getRfmCustomers()) {
            writer.write(String.format("%s,%s,%d,%d,%s,%d,%d,%d,%s,%s\n",
                    escapeCsv(c.getCustomerId()),
                    escapeCsv(c.getCustomerName()),
                    c.getRecencyDays(),
                    c.getFrequency(),
                    AnalyticsMathUtils.roundMoney(c.getMonetary()),
                    c.getrScore(),
                    c.getfScore(),
                    c.getmScore(),
                    c.getRfmScore(),
                    escapeCsv(c.getSegment())
            ));
        }
        writer.flush();
    }

    @Transactional(readOnly = true)
    public void exportInventoryHealthCsv(Long workspaceId, DateRangeFilter filter, Writer writer) throws IOException {
        writer.write("Product ID,Product Name,Category,Current Stock,Safety Reorder Level,Suggested Restock Order,Unit Cost,Estimated Restock Cost,Urgency\n");

        InventoryIntelligenceResponse response = inventoryIntelligenceService.getInventoryIntelligence(workspaceId, filter);
        for (ReorderRecommendationDto r : response.getReorderRecommendations()) {
            writer.write(String.format("%s,%s,%s,%d,%d,%d,%s,%s,%s\n",
                    escapeCsv(r.getProductId()),
                    escapeCsv(r.getProductName()),
                    escapeCsv(r.getCategory()),
                    r.getCurrentStock(),
                    r.getReorderLevel(),
                    r.getSuggestedQuantity(),
                    AnalyticsMathUtils.roundMoney(r.getUnitCost()),
                    AnalyticsMathUtils.roundMoney(r.getEstimatedRestockCost()),
                    escapeCsv(r.getUrgency())
            ));
        }
        writer.flush();
    }

    @Transactional(readOnly = true)
    public void exportRecommendationsCsv(Long workspaceId, Writer writer) throws IOException {
        writer.write("ID,Category,Severity,Priority,Rule Identifier,Title,Description,Action Prompt,Status,Generated At\n");

        List<BusinessRecommendation> recs = businessRecommendationService.getRecommendations(workspaceId, null, null);
        for (BusinessRecommendation r : recs) {
            writer.write(String.format("%d,%s,%s,%d,%s,%s,%s,%s,%s,%s\n",
                    r.getId(),
                    r.getCategory().name(),
                    r.getSeverity().name(),
                    r.getPriority(),
                    escapeCsv(r.getRuleIdentifier()),
                    escapeCsv(r.getTitle()),
                    escapeCsv(r.getDescription()),
                    escapeCsv(r.getActionPrompt()),
                    r.getStatus().name(),
                    r.getGeneratedAt() != null ? r.getGeneratedAt().toString() : ""
            ));
        }
        writer.flush();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
