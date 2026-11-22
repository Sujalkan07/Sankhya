package com.sankhya.ml.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ml.entity.DemandPredictionEntity;
import com.sankhya.ml.entity.MLModel;
import com.sankhya.ml.entity.MLModelExecution;
import com.sankhya.ml.enums.*;
import com.sankhya.ml.features.FeatureEngineeringService;
import com.sankhya.ml.features.ProductFeatureVector;
import com.sankhya.ml.repository.DemandPredictionRepository;
import com.sankhya.ml.repository.MLModelExecutionRepository;
import com.sankhya.ml.repository.MLModelRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProductDemandClassificationService {

    private static final Logger log = LoggerFactory.getLogger(ProductDemandClassificationService.class);

    private final FeatureEngineeringService featureEngineeringService;
    private final MLModelRepository mlModelRepository;
    private final MLModelExecutionRepository mlModelExecutionRepository;
    private final DemandPredictionRepository demandPredictionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public ProductDemandClassificationService(FeatureEngineeringService featureEngineeringService,
                                             MLModelRepository mlModelRepository,
                                             MLModelExecutionRepository mlModelExecutionRepository,
                                             DemandPredictionRepository demandPredictionRepository,
                                             WorkspaceRepository workspaceRepository,
                                             ObjectMapper objectMapper) {
        this.featureEngineeringService = featureEngineeringService;
        this.mlModelRepository = mlModelRepository;
        this.mlModelExecutionRepository = mlModelExecutionRepository;
        this.demandPredictionRepository = demandPredictionRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MLModelExecution trainAndClassify(Long workspaceId, int numTrees) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        MLModel model = mlModelRepository.findByWorkspaceIdAndAlgorithm(workspaceId, AlgorithmType.RANDOM_FOREST)
                .orElseGet(() -> {
                    MLModel m = new MLModel();
                    m.setWorkspace(workspace);
                    m.setModelName("Random Forest Product Demand Classification");
                    m.setAlgorithm(AlgorithmType.RANDOM_FOREST);
                    m.setModelType(ModelType.CLASSIFICATION);
                    m.setVersion("v1.0.0");
                    m.setDescription("Classifies product SKU demand intensity and stockout vulnerability using ensemble decision trees.");
                    m.setStatus(ModelStatus.READY);
                    return mlModelRepository.save(m);
                });

        model.setStatus(ModelStatus.TRAINING);
        mlModelRepository.save(model);

        MLModelExecution execution = new MLModelExecution();
        execution.setModel(model);
        execution.setWorkspace(workspace);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        execution = mlModelExecutionRepository.save(execution);

        long startMs = System.currentTimeMillis();

        try {
            List<ProductFeatureVector> vectors = featureEngineeringService.prepareProductDemandFeatures(workspaceId);
            if (vectors.isEmpty()) {
                throw new IllegalStateException("No product records available in workspace for demand classification.");
            }

            int n = vectors.size();
            double[][] allFeatures = new double[n][];
            DemandClass[] allLabels = new DemandClass[n];
            for (int i = 0; i < n; i++) {
                allFeatures[i] = vectors.get(i).toArray();
                allLabels[i] = vectors.get(i).getLabel();
            }

            // Train / Test split (80% train, 20% test)
            int trainSize = Math.max(1, (int) (n * 0.8));
            int testSize = n - trainSize;

            double[][] trainFeatures = new double[trainSize][];
            DemandClass[] trainLabels = new DemandClass[trainSize];
            for (int i = 0; i < trainSize; i++) {
                trainFeatures[i] = allFeatures[i];
                trainLabels[i] = allLabels[i];
            }

            double[][] testFeatures = new double[testSize > 0 ? testSize : trainSize][];
            DemandClass[] testLabels = new DemandClass[testSize > 0 ? testSize : trainSize];
            if (testSize > 0) {
                for (int i = 0; i < testSize; i++) {
                    testFeatures[i] = allFeatures[trainSize + i];
                    testLabels[i] = allLabels[trainSize + i];
                }
            } else {
                testFeatures = trainFeatures;
                testLabels = trainLabels;
            }

            String[] featureNames = {"totalUnitsSold", "dailyVelocity", "unitPrice", "currentStock"};
            RandomForestClassifier rf = new RandomForestClassifier(numTrees, 5, 2, featureNames.length);
            rf.fit(trainFeatures, trainLabels);

            RandomForestClassifier.EvaluationMetrics eval = rf.evaluate(testFeatures, testLabels, featureNames);

            // Delete old predictions
            List<DemandPredictionEntity> oldPredictions = demandPredictionRepository.findByExecutionIdOrderByConfidenceDesc(execution.getId());
            if (!oldPredictions.isEmpty()) {
                demandPredictionRepository.deleteAllInBatch(oldPredictions);
            }

            // Generate predictions for all products
            List<DemandPredictionEntity> predictions = new ArrayList<>();
            for (ProductFeatureVector vec : vectors) {
                RandomForestClassifier.PredictionResult res = rf.predict(vec.toArray());

                StockoutRiskLevel riskLevel;
                double predicted7dUnits = vec.getDailyVelocity() * 7.0;
                if (res.predictedClass == DemandClass.HIGH && vec.getCurrentStock() < predicted7dUnits) {
                    riskLevel = StockoutRiskLevel.HIGH;
                } else if (res.predictedClass == DemandClass.MEDIUM && vec.getCurrentStock() < (predicted7dUnits * 0.75)) {
                    riskLevel = StockoutRiskLevel.MODERATE;
                } else {
                    riskLevel = StockoutRiskLevel.LOW;
                }

                DemandPredictionEntity pred = new DemandPredictionEntity();
                pred.setWorkspace(workspace);
                pred.setExecution(execution);
                pred.setProductId(vec.getProductId());
                pred.setProductCode(vec.getProductCode());
                pred.setProductName(vec.getProductName());
                pred.setCategory(vec.getCategory());
                pred.setPredictedDemandClass(res.predictedClass);
                pred.setConfidence(res.confidence);
                pred.setRecentDailyVelocity(vec.getDailyVelocity());
                pred.setPredictedUnitsNext7d(predicted7dUnits);
                pred.setStockoutRiskLevel(riskLevel);
                pred.setCreatedAt(LocalDateTime.now());
                predictions.add(pred);
            }
            demandPredictionRepository.saveAll(predictions);

            // Persist metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("accuracy", eval.accuracy);
            metrics.put("precision", eval.precision);
            metrics.put("recall", eval.recall);
            metrics.put("f1Score", eval.f1Score);
            metrics.put("featureImportance", eval.featureImportanceMap);
            metrics.put("totalProductsClassified", n);

            Map<String, Object> hyperparams = new HashMap<>();
            hyperparams.put("numTrees", numTrees);
            hyperparams.put("maxDepth", 5);
            hyperparams.put("minSamplesSplit", 2);

            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(System.currentTimeMillis() - startMs);
            execution.setTrainRecordCount(trainSize);
            execution.setTestRecordCount(testSize);
            execution.setHyperparametersJson(objectMapper.writeValueAsString(hyperparams));
            execution.setMetricsJson(objectMapper.writeValueAsString(metrics));

            model.setStatus(ModelStatus.EVALUATED);
            model.setUpdatedAt(LocalDateTime.now());
            mlModelRepository.save(model);

            return mlModelExecutionRepository.save(execution);

        } catch (Exception e) {
            log.error("Failed to train Random Forest model for workspace {}", workspaceId, e);
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(System.currentTimeMillis() - startMs);
            execution.setErrorMessage(e.getMessage());
            model.setStatus(ModelStatus.FAILED);
            mlModelRepository.save(model);
            return mlModelExecutionRepository.save(execution);
        }
    }
}
