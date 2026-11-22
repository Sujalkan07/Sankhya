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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetQueryService {

    private final DatasetRepository datasetRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;

    public DatasetQueryService(DatasetRepository datasetRepository,
                               SalesTransactionRepository salesTransactionRepository,
                               ProductRepository productRepository,
                               CustomerRepository customerRepository,
                               InventorySnapshotRepository inventorySnapshotRepository) {
        this.datasetRepository = datasetRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
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

        summary.put("totalDatasets", DatasetType.values().length);
        summary.put("activeDatasets", activeDatasets);
        summary.put("totalRecords", totalRecords);
        summary.put("categoryCounts", categoryCounts);
        summary.put("isReadyForAnalytics", activeDatasets >= 3); // needs at least sales, products, customers

        return summary;
    }

    @Transactional(readOnly = true)
    public boolean hasRequiredAnalyticsData(Long workspaceId) {
        return isSalesAnalyticsReady(workspaceId) && isProductIntelligenceReady(workspaceId);
    }

    @Transactional(readOnly = true)
    public boolean isSalesAnalyticsReady(Long workspaceId) {
        return salesTransactionRepository.countByWorkspaceId(workspaceId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isProductIntelligenceReady(Long workspaceId) {
        return productRepository.countByWorkspaceId(workspaceId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isCustomerIntelligenceReady(Long workspaceId) {
        return customerRepository.countByWorkspaceId(workspaceId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isInventoryIntelligenceReady(Long workspaceId) {
        return inventorySnapshotRepository.countByWorkspaceId(workspaceId) > 0;
    }

    @Transactional(readOnly = true)
    public AnalyticsReadinessResponse getAnalyticsReadiness(Long workspaceId) {
        AnalyticsReadinessResponse response = new AnalyticsReadinessResponse();

        long salesCount = salesTransactionRepository.countByWorkspaceId(workspaceId);
        long productCount = productRepository.countByWorkspaceId(workspaceId);
        long customerCount = customerRepository.countByWorkspaceId(workspaceId);
        long inventoryCount = inventorySnapshotRepository.countByWorkspaceId(workspaceId);

        response.setSalesRecordCount(salesCount);
        response.setProductRecordCount(productCount);
        response.setCustomerRecordCount(customerCount);
        response.setInventoryRecordCount(inventoryCount);

        boolean salesReady = salesCount > 0;
        boolean productReady = productCount > 0;
        boolean customerReady = customerCount > 0;
        boolean inventoryReady = inventoryCount > 0;

        response.setSalesReady(salesReady);
        response.setProductReady(productReady);
        response.setCustomerReady(customerReady);
        response.setInventoryReady(inventoryReady);

        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!salesReady) {
            missing.add("Sales Transactions dataset is missing or empty.");
        }
        if (!productReady) {
            missing.add("Product Catalog dataset is missing or empty.");
        }
        if (!customerReady) {
            missing.add("Customer Profiles dataset is missing or empty.");
        }
        if (!inventoryReady) {
            missing.add("Inventory Snapshots dataset is missing or empty.");
        }

        // Cross-domain quality checks
        if (salesReady && !productReady) {
            warnings.add("Sales transactions are loaded without a product catalog. Cost of Goods Sold and profit margins cannot be calculated accurately.");
        }
        if (salesReady && !customerReady) {
            warnings.add("Sales transactions are loaded without customer profiles. Customer segmentation and RFM analysis will only use transaction references.");
        }
        if (inventoryReady && !productReady) {
            warnings.add("Inventory snapshots lack catalog metadata. Product names and categories may fall back to identifiers.");
        }

        response.setMissingDependencies(missing);
        response.setDataQualityWarnings(warnings);
        response.setOverallReady(salesReady && productReady);
        response.setRecommendationsReady(salesReady || inventoryReady);

        return response;
    }
}
