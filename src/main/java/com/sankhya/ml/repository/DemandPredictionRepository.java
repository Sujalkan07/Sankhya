package com.sankhya.ml.repository;

import com.sankhya.ml.entity.DemandPredictionEntity;
import com.sankhya.ml.enums.DemandClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemandPredictionRepository extends JpaRepository<DemandPredictionEntity, Long> {
    List<DemandPredictionEntity> findByExecutionIdOrderByConfidenceDesc(Long executionId);
    List<DemandPredictionEntity> findByWorkspaceIdOrderByPredictedUnitsNext7dDesc(Long workspaceId);
    List<DemandPredictionEntity> findByWorkspaceIdAndPredictedDemandClass(Long workspaceId, DemandClass demandClass);
    void deleteByExecutionId(Long executionId);
}
