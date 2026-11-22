package com.sankhya.ml.execution;

import com.sankhya.ml.association.MarketBasketAnalysisService;
import com.sankhya.ml.classification.ProductDemandClassificationService;
import com.sankhya.ml.clustering.CustomerClusteringService;
import com.sankhya.ml.entity.MLModelExecution;
import com.sankhya.ml.forecasting.SalesForecastingService;
import com.sankhya.ml.registry.ModelRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MLJobService {

    private static final Logger log = LoggerFactory.getLogger(MLJobService.class);

    private final CustomerClusteringService customerClusteringService;
    private final MarketBasketAnalysisService marketBasketAnalysisService;
    private final ProductDemandClassificationService productDemandClassificationService;
    private final SalesForecastingService salesForecastingService;
    private final ModelRegistryService modelRegistryService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, String> activeJobs = new ConcurrentHashMap<>();

    public MLJobService(CustomerClusteringService customerClusteringService,
                        MarketBasketAnalysisService marketBasketAnalysisService,
                        ProductDemandClassificationService productDemandClassificationService,
                        SalesForecastingService salesForecastingService,
                        ModelRegistryService modelRegistryService) {
        this.customerClusteringService = customerClusteringService;
        this.marketBasketAnalysisService = marketBasketAnalysisService;
        this.productDemandClassificationService = productDemandClassificationService;
        this.salesForecastingService = salesForecastingService;
        this.modelRegistryService = modelRegistryService;
    }

    public MLModelExecution executeKMeans(Long workspaceId, int k) {
        modelRegistryService.ensureDefaultModelsForWorkspace(workspaceId);
        return customerClusteringService.trainAndCluster(workspaceId, k);
    }

    public MLModelExecution executeApriori(Long workspaceId, double minSupport, double minConfidence, double minLift) {
        modelRegistryService.ensureDefaultModelsForWorkspace(workspaceId);
        return marketBasketAnalysisService.mineAndPersistRules(workspaceId, minSupport, minConfidence, minLift);
    }

    public MLModelExecution executeRandomForest(Long workspaceId, int numTrees) {
        modelRegistryService.ensureDefaultModelsForWorkspace(workspaceId);
        return productDemandClassificationService.trainAndClassify(workspaceId, numTrees);
    }

    public MLModelExecution executeLinearRegression(Long workspaceId, int horizonDays) {
        modelRegistryService.ensureDefaultModelsForWorkspace(workspaceId);
        return salesForecastingService.trainAndForecast(workspaceId, horizonDays);
    }

    public CompletableFuture<Void> trainAllModelsAsync(Long workspaceId) {
        String jobKey = "workspace-" + workspaceId;
        activeJobs.put(jobKey, "RUNNING");

        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting automated training for all ML models in workspace {}", workspaceId);
                modelRegistryService.ensureDefaultModelsForWorkspace(workspaceId);

                executeKMeans(workspaceId, 4);
                executeApriori(workspaceId, 0.01, 0.20, 1.05);
                executeRandomForest(workspaceId, 30);
                executeLinearRegression(workspaceId, 14);

                activeJobs.put(jobKey, "COMPLETED");
                log.info("Successfully trained all ML models for workspace {}", workspaceId);
            } catch (Exception e) {
                activeJobs.put(jobKey, "FAILED: " + e.getMessage());
                log.error("Error during multi-model training for workspace {}", workspaceId, e);
            }
        }, executorService);
    }

    public String getJobStatus(Long workspaceId) {
        return activeJobs.getOrDefault("workspace-" + workspaceId, "IDLE");
    }
}
