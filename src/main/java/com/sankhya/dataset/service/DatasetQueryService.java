package com.sankhya.dataset.service;

import com.sankhya.analytics.common.dto.AnalyticsReadinessResponse;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.repository.DatasetRepository;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetQueryService {

    private final DatasetRepository datasetRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final com.sankhya.dataset.repository.UnifiedSalesMartRepository unifiedSalesMartRepository;

    public DatasetQueryService(DatasetRepository datasetRepository,
                               SalesTransactionRepository salesTransactionRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               InventorySnapshotRepository inventorySnapshotRepository,
                               com.sankhya.dataset.repository.UnifiedSalesMartRepository unifiedSalesMartRepository) {
        this.datasetRepository = datasetRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDatasetHealthSummary(Long workspaceId) {
        List<Dataset> datasets = datasetRepository.findByWorkspaceId(workspaceId);
        Map<String, Object> summary = new HashMap<>();

        long totalRecords = 0;
        int activeDatasets = 0;
        Map<String, Long> categoryCounts = new HashMap<>();

        for (Dataset d : datasets) {
            categoryCounts.put(d.getDatasetType().name(), (long) d.getRecordCount());
            totalRecords += d.getRecordCount();
            if (d.getRecordCount() > 0) {
                activeDatasets++;
            }
        }

        long martCount = unifiedSalesMartRepository.countByWorkspaceId(workspaceId);
        if (martCount > 0) {
            boolean alreadyCounted = datasets.stream().anyMatch(d -> d.getDatasetType() == DatasetType.UNIFIED_MART && d.getRecordCount() > 0);
            categoryCounts.put(DatasetType.UNIFIED_MART.name(), martCount);
            totalRecords = Math.max(totalRecords, martCount);
            if (!alreadyCounted) {
                activeDatasets++;
            }
        }

        summary.put("totalDatasets", DatasetType.values().length);
        summary.put("activeDatasets", activeDatasets);
        summary.put("totalRecords", totalRecords);
        summary.put("categoryCounts", categoryCounts);
        summary.put("isReadyForAnalytics", activeDatasets >= 1 && (martCount > 0 || hasRequiredAnalyticsData(workspaceId)));

        return summary;
    }

    @Transactional(readOnly = true)
    public boolean hasRequiredAnalyticsData(Long workspaceId) {
        return isSalesAnalyticsReady(workspaceId) && isProductIntelligenceReady(workspaceId);
    }

    @Transactional(readOnly = true)
    public boolean isSalesAnalyticsReady(Long workspaceId) {
        return salesTransactionRepository.countByWorkspaceId(workspaceId) > 0 || unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isProductIntelligenceReady(Long workspaceId) {
        return productRepository.countByWorkspaceId(workspaceId) > 0 || unifiedSalesMartRepository.countTotalProductsByWorkspaceId(workspaceId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isCustomerIntelligenceReady(Long workspaceId) {
        return customerRepository.countByWorkspaceId(workspaceId) > 0 || unifiedSalesMartRepository.countTotalCustomersByWorkspaceId(workspaceId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isInventoryIntelligenceReady(Long workspaceId) {
        return inventorySnapshotRepository.countByWorkspaceId(workspaceId) > 0 || unifiedSalesMartRepository.countInventoryObservationsByWorkspaceId(workspaceId) > 0;
    }

    @Transactional(readOnly = true)
    public AnalyticsReadinessResponse getAnalyticsReadiness(Long workspaceId) {
        AnalyticsReadinessResponse response = new AnalyticsReadinessResponse();

        long martCount = unifiedSalesMartRepository.countByWorkspaceId(workspaceId);
        long salesCount;
        long productCount;
        long customerCount;
        long inventoryCount;
        long distinctTxCount;
        LocalDateTime minDate = null;
        LocalDateTime maxDate = null;
        long dailyBucketCount = 0;

        if (martCount > 0) {
            response.setDataSource("UNIFIED_SALES_MART");
            salesCount = martCount;
            distinctTxCount = unifiedSalesMartRepository.countDistinctTransactionsByWorkspaceId(workspaceId);
            productCount = unifiedSalesMartRepository.countTotalProductsByWorkspaceId(workspaceId);
            customerCount = unifiedSalesMartRepository.countTotalCustomersByWorkspaceId(workspaceId);
            inventoryCount = unifiedSalesMartRepository.findLatestInventoryPerProductByWorkspaceId(workspaceId)
                    .stream().filter(r -> r[6] != null).count();
            minDate = unifiedSalesMartRepository.findMinTransactionDateByWorkspaceId(workspaceId);
            maxDate = unifiedSalesMartRepository.findMaxTransactionDateByWorkspaceId(workspaceId);
            if (minDate != null && maxDate != null) {
                dailyBucketCount = unifiedSalesMartRepository.findDailyAggregationsBetween(
                        workspaceId, minDate, maxDate).size();
            }
        } else {
            salesCount = salesTransactionRepository.countByWorkspaceId(workspaceId);
            distinctTxCount = salesCount > 0 ? salesTransactionRepository.countDistinctTransactionsByWorkspaceId(workspaceId) : 0L;
            productCount = productRepository.countByWorkspaceId(workspaceId);
            customerCount = customerRepository.countByWorkspaceId(workspaceId);
            inventoryCount = inventorySnapshotRepository.countByWorkspaceId(workspaceId);
            response.setDataSource((salesCount > 0 || productCount > 0 || customerCount > 0 || inventoryCount > 0)
                    ? "NORMALIZED_TABLES" : "NONE");
            if (salesCount > 0) {
                dailyBucketCount = salesTransactionRepository.findDailyAggregationsBetween(
                        workspaceId, LocalDateTime.now().minusYears(5), LocalDateTime.now().plusDays(1)).size();
            }
        }

        response.setSalesRecordCount(salesCount);
        response.setProductRecordCount(productCount);
        response.setCustomerRecordCount(customerCount);
        response.setInventoryRecordCount(inventoryCount);
        response.setDistinctTransactionCount(distinctTxCount);
        response.setMinTransactionDate(minDate);
        response.setMaxTransactionDate(maxDate);

        boolean salesReady = salesCount > 0;
        boolean productReady = productCount > 0 && salesCount > 0;
        boolean customerReady = customerCount > 0;
        boolean inventoryReady = inventoryCount > 0;

        response.setSalesReady(salesReady);
        response.setProductReady(productReady);
        response.setCustomerReady(customerReady);
        response.setInventoryReady(inventoryReady);

        // ML Module-specific readiness checks
        boolean aprioriReady = distinctTxCount >= 5;
        boolean kmeansReady = customerCount >= 4 && salesReady;
        boolean randomForestReady = productCount >= 5 && salesReady;
        boolean linearRegressionReady = salesReady && (dailyBucketCount >= 7 ||
                (minDate != null && maxDate != null && ChronoUnit.DAYS.between(minDate.toLocalDate(), maxDate.toLocalDate()) >= 7));

        response.setAprioriReady(aprioriReady);
        response.setKmeansReady(kmeansReady);
        response.setRandomForestReady(randomForestReady);
        response.setLinearRegressionReady(linearRegressionReady);

        Map<String, String> blockReasons = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!salesReady) {
            missing.add("Sales Transactions dataset is missing or empty.");
            blockReasons.put("SALES_ANALYTICS", "No sales transaction line items loaded.");
        }
        if (productCount == 0) {
            missing.add("Product Catalog dataset is missing or empty.");
            blockReasons.put("PRODUCT_INTELLIGENCE", "No product catalog SKUs loaded.");
        } else if (!salesReady) {
            blockReasons.put("PRODUCT_INTELLIGENCE", "Products loaded, but sales transactions are required for velocity and margin analytics.");
        }
        if (!customerReady) {
            missing.add("Customer Profiles dataset is missing or empty.");
            blockReasons.put("CUSTOMER_INTELLIGENCE", "No customer identifiers present.");
        }
        if (!inventoryReady) {
            missing.add("Inventory Snapshots dataset is missing or empty.");
            blockReasons.put("INVENTORY_INTELLIGENCE", "No valid closing stock observations present.");
        }
        if (!aprioriReady) {
            blockReasons.put("APRIORI", "At least 5 distinct multi-item transactions required (found " + distinctTxCount + ").");
        }
        if (!kmeansReady) {
            blockReasons.put("K_MEANS", "At least 4 identified customers with transaction history required (found " + customerCount + ").");
        }
        if (!randomForestReady) {
            blockReasons.put("RANDOM_FOREST", "At least 5 active SKUs with sales history required (found " + productCount + ").");
        }
        if (!linearRegressionReady) {
            blockReasons.put("LINEAR_REGRESSION", "At least 7 distinct daily chronological sales observations required (found " + dailyBucketCount + ").");
        }

        // Cross-domain quality checks
        if (salesReady && productCount == 0) {
            warnings.add("Sales transactions are loaded without a product catalog. Cost of Goods Sold and profit margins cannot be calculated accurately.");
        }
        if (salesReady && !customerReady) {
            warnings.add("Sales transactions are loaded without customer profiles. Customer segmentation and RFM analysis will only use transaction references.");
        }
        if (inventoryReady && productCount == 0) {
            warnings.add("Inventory snapshots lack catalog metadata. Product names and categories may fall back to identifiers.");
        }

        response.setMissingDependencies(missing);
        response.setDataQualityWarnings(warnings);
        response.setModuleBlockReasons(blockReasons);
        response.setOverallReady(salesReady && productCount > 0);
        response.setRecommendationsReady(salesReady || inventoryReady);

        return response;
    }
}
