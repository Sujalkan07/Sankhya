package com.sankhya.ml.repository;

import com.sankhya.ml.entity.MLModelExecution;
import com.sankhya.ml.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MLModelExecutionRepository extends JpaRepository<MLModelExecution, Long> {
    List<MLModelExecution> findByWorkspaceIdOrderByStartedAtDesc(Long workspaceId);
    List<MLModelExecution> findByModelIdOrderByStartedAtDesc(Long modelId);
    Optional<MLModelExecution> findFirstByModelIdAndStatusOrderByStartedAtDesc(Long modelId, ExecutionStatus status);
    Optional<MLModelExecution> findFirstByWorkspaceIdAndStatusOrderByStartedAtDesc(Long workspaceId, ExecutionStatus status);
    long countByWorkspaceIdAndStatus(Long workspaceId, ExecutionStatus status);
}
