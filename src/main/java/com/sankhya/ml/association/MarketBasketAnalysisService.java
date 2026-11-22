package com.sankhya.ml.association;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.ml.entity.AssociationRuleEntity;
import com.sankhya.ml.entity.MLModel;
import com.sankhya.ml.entity.MLModelExecution;
import com.sankhya.ml.enums.AlgorithmType;
import com.sankhya.ml.enums.ExecutionStatus;
import com.sankhya.ml.enums.ModelStatus;
import com.sankhya.ml.enums.ModelType;
import com.sankhya.ml.features.FeatureEngineeringService;
import com.sankhya.ml.repository.AssociationRuleRepository;
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
public class MarketBasketAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MarketBasketAnalysisService.class);

    private final FeatureEngineeringService featureEngineeringService;
    private final AprioriService aprioriService;
    private final MLModelRepository mlModelRepository;
    private final MLModelExecutionRepository mlModelExecutionRepository;
    private final AssociationRuleRepository associationRuleRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public MarketBasketAnalysisService(FeatureEngineeringService featureEngineeringService,
                                       AprioriService aprioriService,
                                       MLModelRepository mlModelRepository,
                                       MLModelExecutionRepository mlModelExecutionRepository,
                                       AssociationRuleRepository associationRuleRepository,
                                       WorkspaceRepository workspaceRepository,
                                       ObjectMapper objectMapper) {
        this.featureEngineeringService = featureEngineeringService;
        this.aprioriService = aprioriService;
        this.mlModelRepository = mlModelRepository;
        this.mlModelExecutionRepository = mlModelExecutionRepository;
        this.associationRuleRepository = associationRuleRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MLModelExecution mineAndPersistRules(Long workspaceId, double minSupport, double minConfidence, double minLift) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        MLModel model = mlModelRepository.findByWorkspaceIdAndAlgorithm(workspaceId, AlgorithmType.APRIORI)
                .orElseGet(() -> {
                    MLModel m = new MLModel();
                    m.setWorkspace(workspace);
                    m.setModelName("Apriori Market Basket Association");
                    m.setAlgorithm(AlgorithmType.APRIORI);
                    m.setModelType(ModelType.ASSOCIATION_RULES);
                    m.setVersion("v1.0.0");
                    m.setDescription("Extracts high-lift cross-sell rules from co-occurring items in multi-item transaction baskets.");
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
            List<Set<String>> baskets = featureEngineeringService.prepareMarketBaskets(workspaceId);
            if (baskets.isEmpty()) {
                throw new IllegalStateException("No multi-item transaction baskets found for workspace.");
            }

            // Adjust minSupport dynamically if transactions are sparse to ensure meaningful rules
            double effectiveSupport = Math.min(minSupport, 5.0 / baskets.size());

            List<AssociationRuleDto> rules = aprioriService.mineRules(
                    baskets, effectiveSupport, minConfidence, minLift
            );

            // Delete old rules for workspace
            List<AssociationRuleEntity> oldRules = associationRuleRepository.findByWorkspaceIdOrderByLiftDesc(workspaceId);
            if (!oldRules.isEmpty()) {
                associationRuleRepository.deleteAllInBatch(oldRules);
            }

            // Save new rules
            List<AssociationRuleEntity> entities = new ArrayList<>();
            for (AssociationRuleDto dto : rules) {
                AssociationRuleEntity entity = new AssociationRuleEntity();
                entity.setWorkspace(workspace);
                entity.setExecution(execution);
                entity.setAntecedent(String.join(", ", dto.getAntecedent()));
                entity.setConsequent(String.join(", ", dto.getConsequent()));
                entity.setSupport(dto.getSupport());
                entity.setConfidence(dto.getConfidence());
                entity.setLift(dto.getLift());
                entity.setConviction(dto.getConviction());
                entity.setRuleType("CROSS_SELL");
                entity.setActionableRecommendation(dto.getRecommendation());
                entity.setCreatedAt(LocalDateTime.now());
                entities.add(entity);
            }
            associationRuleRepository.saveAll(entities);

            // Record metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalBasketsAnalyzed", baskets.size());
            metrics.put("rulesFound", rules.size());
            if (!rules.isEmpty()) {
                metrics.put("maxLift", rules.get(0).getLift());
                metrics.put("avgLift", rules.stream().mapToDouble(AssociationRuleDto::getLift).average().orElse(0.0));
                metrics.put("avgConfidence", rules.stream().mapToDouble(AssociationRuleDto::getConfidence).average().orElse(0.0));
            }

            Map<String, Object> hyperparams = new HashMap<>();
            hyperparams.put("minSupport", effectiveSupport);
            hyperparams.put("minConfidence", minConfidence);
            hyperparams.put("minLift", minLift);

            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(System.currentTimeMillis() - startMs);
            execution.setTrainRecordCount(baskets.size());
            execution.setHyperparametersJson(objectMapper.writeValueAsString(hyperparams));
            execution.setMetricsJson(objectMapper.writeValueAsString(metrics));

            model.setStatus(ModelStatus.EVALUATED);
            model.setUpdatedAt(LocalDateTime.now());
            mlModelRepository.save(model);

            return mlModelExecutionRepository.save(execution);

        } catch (Exception e) {
            log.error("Failed to mine association rules for workspace {}", workspaceId, e);
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
