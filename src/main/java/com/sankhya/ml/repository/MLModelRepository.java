package com.sankhya.ml.repository;

import com.sankhya.ml.entity.MLModel;
import com.sankhya.ml.enums.AlgorithmType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MLModelRepository extends JpaRepository<MLModel, Long> {
    List<MLModel> findByWorkspaceIdOrderByIdAsc(Long workspaceId);
    Optional<MLModel> findByWorkspaceIdAndAlgorithm(Long workspaceId, AlgorithmType algorithm);
    Optional<MLModel> findByWorkspaceIdAndAlgorithmAndActiveTrue(Long workspaceId, AlgorithmType algorithm);
    List<MLModel> findByWorkspaceIdAndActiveTrue(Long workspaceId);
}
