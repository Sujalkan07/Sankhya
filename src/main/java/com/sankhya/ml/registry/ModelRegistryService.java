package com.sankhya.ml.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ml.entity.MLModel;
import com.sankhya.ml.entity.MLModelExecution;
import com.sankhya.ml.enums.AlgorithmType;
import com.sankhya.ml.enums.ExecutionStatus;
import com.sankhya.ml.enums.ModelStatus;
import com.sankhya.ml.enums.ModelType;
import com.sankhya.ml.repository.MLModelExecutionRepository;
import com.sankhya.ml.repository.MLModelRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ModelRegistryService {

    private final MLModelRepository mlModelRepository;
    private final MLModelExecutionRepository mlModelExecutionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public ModelRegistryService(MLModelRepository mlModelRepository,
                                MLModelExecutionRepository mlModelExecutionRepository,
                                WorkspaceRepository workspaceRepository,
                                ObjectMapper objectMapper) {
        this.mlModelRepository = mlModelRepository;
        this.mlModelExecutionRepository = mlModelExecutionRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ensureDefaultModelsForWorkspace(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) return;

        createIfAbsent(workspace, "K-Means Customer Segmentation", AlgorithmType.K_MEANS,
                ModelType.CLUSTERING, "Unsupervised clustering across customer RFM, ATV, and purchase habits.");

        createIfAbsent(workspace, "Apriori Market Basket Association", AlgorithmType.APRIORI,
                ModelType.ASSOCIATION_RULES, "High-lift cross-sell rule mining across multi-item transactions.");

        createIfAbsent(workspace, "Random Forest Product Demand Classification", AlgorithmType.RANDOM_FOREST,
                ModelType.CLASSIFICATION, "Ensemble decision tree classifier predicting SKU velocity and stockout vulnerability.");

        createIfAbsent(workspace, "Linear Regression Sales Forecasting", AlgorithmType.LINEAR_REGRESSION,
                ModelType.FORECASTING, "Autoregressive multi-step daily revenue forecasting with calendar seasonality.");
    }

    private void createIfAbsent(Workspace workspace, String name, AlgorithmType algo, ModelType type, String desc) {
        if (mlModelRepository.findByWorkspaceIdAndAlgorithm(workspace.getId(), algo).isEmpty()) {
            MLModel m = new MLModel();
            m.setWorkspace(workspace);
            m.setModelName(name);
            m.setAlgorithm(algo);
            m.setModelType(type);
            m.setVersion("v1.0.0");
            m.setDescription(desc);
            m.setStatus(ModelStatus.READY);
            m.setActive(true);
            m.setCreatedAt(LocalDateTime.now());
            mlModelRepository.save(m);
        }
    }

    @Transactional(readOnly = true)
    public List<MLModel> getModelsForWorkspace(Long workspaceId) {
        return mlModelRepository.findByWorkspaceIdOrderByIdAsc(workspaceId);
    }

    @Transactional(readOnly = true)
    public Optional<MLModelExecution> getLatestExecution(Long modelId) {
        return mlModelExecutionRepository.findFirstByModelIdAndStatusOrderByStartedAtDesc(modelId, ExecutionStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> parseMetrics(String metricsJson) {
        if (metricsJson == null || metricsJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(metricsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @Transactional(readOnly = true)
    public List<MLModelExecution> getExecutionHistory(Long workspaceId) {
        return mlModelExecutionRepository.findByWorkspaceIdOrderByStartedAtDesc(workspaceId);
    }
}
