package com.sankhya.ml.repository;

import com.sankhya.ml.entity.AssociationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociationRuleRepository extends JpaRepository<AssociationRuleEntity, Long> {
    List<AssociationRuleEntity> findByExecutionIdOrderByLiftDesc(Long executionId);
    List<AssociationRuleEntity> findByWorkspaceIdOrderByLiftDesc(Long workspaceId);
    List<AssociationRuleEntity> findTop20ByWorkspaceIdOrderByLiftDesc(Long workspaceId);
    void deleteByExecutionId(Long executionId);
}
