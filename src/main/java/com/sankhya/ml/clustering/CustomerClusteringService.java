package com.sankhya.ml.clustering;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ml.entity.CustomerClusterAssignment;
import com.sankhya.ml.entity.MLModel;
import com.sankhya.ml.entity.MLModelExecution;
import com.sankhya.ml.enums.AlgorithmType;
import com.sankhya.ml.enums.ExecutionStatus;
import com.sankhya.ml.enums.ModelStatus;
import com.sankhya.ml.enums.ModelType;
import com.sankhya.ml.features.CustomerFeatureVector;
import com.sankhya.ml.features.FeatureEngineeringService;
import com.sankhya.ml.repository.CustomerClusterAssignmentRepository;
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
public class CustomerClusteringService {

    private static final Logger log = LoggerFactory.getLogger(CustomerClusteringService.class);

    private final FeatureEngineeringService featureEngineeringService;
    private final KMeansService kMeansService;
    private final MLModelRepository mlModelRepository;
    private final MLModelExecutionRepository mlModelExecutionRepository;
    private final CustomerClusterAssignmentRepository customerClusterAssignmentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public CustomerClusteringService(FeatureEngineeringService featureEngineeringService,
                                     KMeansService kMeansService,
                                     MLModelRepository mlModelRepository,
                                     MLModelExecutionRepository mlModelExecutionRepository,
                                     CustomerClusterAssignmentRepository customerClusterAssignmentRepository,
                                     WorkspaceRepository workspaceRepository,
                                     ObjectMapper objectMapper) {
        this.featureEngineeringService = featureEngineeringService;
        this.kMeansService = kMeansService;
        this.mlModelRepository = mlModelRepository;
        this.mlModelExecutionRepository = mlModelExecutionRepository;
        this.customerClusterAssignmentRepository = customerClusterAssignmentRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MLModelExecution trainAndCluster(Long workspaceId, int k) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 1. Get or create Model entity
        MLModel model = mlModelRepository.findByWorkspaceIdAndAlgorithm(workspaceId, AlgorithmType.K_MEANS)
                .orElseGet(() -> {
                    MLModel m = new MLModel();
                    m.setWorkspace(workspace);
                    m.setModelName("K-Means Customer Segmentation");
                    m.setAlgorithm(AlgorithmType.K_MEANS);
                    m.setModelType(ModelType.CLUSTERING);
                    m.setVersion("v1.0.0");
                    m.setDescription("Unsupervised K-Means clustering across customer recency, frequency, monetary, and ATV.");
                    m.setStatus(ModelStatus.READY);
                    return mlModelRepository.save(m);
                });

        model.setStatus(ModelStatus.TRAINING);
        mlModelRepository.save(model);

        // 2. Create Execution record
        MLModelExecution execution = new MLModelExecution();
        execution.setModel(model);
        execution.setWorkspace(workspace);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartedAt(LocalDateTime.now());
        execution = mlModelExecutionRepository.save(execution);

        long startMs = System.currentTimeMillis();

        try {
            // 3. Extract features
            List<CustomerFeatureVector> vectors = featureEngineeringService.prepareCustomerFeatures(workspaceId);
            if (vectors.isEmpty()) {
                throw new IllegalStateException("No customer data available in workspace for clustering.");
            }

            int actualK = Math.min(k, Math.max(2, vectors.size()));
            double[][] rawData = new double[vectors.size()][];
            for (int i = 0; i < vectors.size(); i++) {
                rawData[i] = vectors.get(i).toArray();
            }

            // 4. Train KMeans Model
            KMeansModel kMeansModel = kMeansService.fit(rawData, actualK, 100);

            // 5. Delete previous assignments for workspace
            List<CustomerClusterAssignment> existing = customerClusterAssignmentRepository.findByWorkspaceIdOrderByClusterIdAsc(workspaceId);
            if (!existing.isEmpty()) {
                customerClusterAssignmentRepository.deleteAllInBatch(existing);
            }

            // 6. Save new assignments
            List<CustomerClusterAssignment> assignments = new ArrayList<>();
            for (CustomerFeatureVector vec : vectors) {
                int clusterId = kMeansModel.predict(vec.toArray());
                double dist = kMeansModel.distanceToCentroid(vec.toArray(), clusterId);

                CustomerClusterAssignment assignment = new CustomerClusterAssignment();
                assignment.setWorkspace(workspace);
                assignment.setExecution(execution);
                assignment.setCustomerId(vec.getCustomerId());
                assignment.setCustomerCode(vec.getCustomerCode());
                assignment.setClusterId(clusterId);
                assignment.setClusterLabel(kMeansModel.getClusterLabels()[clusterId]);
                assignment.setRecencyDays(vec.getRecency());
                assignment.setFrequency(vec.getFrequency());
                assignment.setMonetary(vec.getMonetary());
                assignment.setAverageTransactionValue(vec.getAverageTransactionValue());
                assignment.setDistanceToCentroid(dist);
                assignment.setAssignedAt(LocalDateTime.now());

                assignments.add(assignment);
            }
            customerClusterAssignmentRepository.saveAll(assignments);

            // 7. Store metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("k", actualK);
            metrics.put("inertia", kMeansModel.getInertia());
            metrics.put("silhouetteScore", kMeansModel.getSilhouetteScore());
            metrics.put("totalCustomersClustered", vectors.size());

            Map<String, Object> hyperparams = new HashMap<>();
            hyperparams.put("maxIterations", 100);
            hyperparams.put("k", actualK);
            hyperparams.put("init", "k-means++");

            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(System.currentTimeMillis() - startMs);
            execution.setTrainRecordCount(vectors.size());
            execution.setHyperparametersJson(objectMapper.writeValueAsString(hyperparams));
            execution.setMetricsJson(objectMapper.writeValueAsString(metrics));

            model.setStatus(ModelStatus.EVALUATED);
            model.setUpdatedAt(LocalDateTime.now());
            mlModelRepository.save(model);

            return mlModelExecutionRepository.save(execution);

        } catch (Exception e) {
            log.error("Failed to train KMeans model for workspace {}", workspaceId, e);
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
