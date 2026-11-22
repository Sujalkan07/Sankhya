package com.sankhya.ml.repository;

import com.sankhya.ml.entity.GeminiInsightEntity;
import com.sankhya.ml.enums.InsightCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeminiInsightRepository extends JpaRepository<GeminiInsightEntity, Long> {
    List<GeminiInsightEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
    Optional<GeminiInsightEntity> findFirstByWorkspaceIdAndCategoryOrderByCreatedAtDesc(Long workspaceId, InsightCategory category);
    List<GeminiInsightEntity> findByWorkspaceIdAndCategoryOrderByCreatedAtDesc(Long workspaceId, InsightCategory category);
}
