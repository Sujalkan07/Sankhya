package com.sankhya.analytics.recommendations.repository;

import com.sankhya.analytics.recommendations.entity.BusinessRecommendation;
import com.sankhya.analytics.recommendations.enums.RecommendationCategory;
import com.sankhya.analytics.recommendations.enums.RecommendationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRecommendationRepository extends JpaRepository<BusinessRecommendation, Long> {

    List<BusinessRecommendation> findByWorkspaceIdOrderByPriorityAscGeneratedAtDesc(Long workspaceId);

    Page<BusinessRecommendation> findByWorkspaceIdOrderByPriorityAscGeneratedAtDesc(Long workspaceId, Pageable pageable);

    List<BusinessRecommendation> findByWorkspaceIdAndStatusOrderByPriorityAsc(Long workspaceId, RecommendationStatus status);

    List<BusinessRecommendation> findByWorkspaceIdAndCategoryOrderByPriorityAsc(Long workspaceId, RecommendationCategory category);

    Optional<BusinessRecommendation> findByIdAndWorkspaceId(Long id, Long workspaceId);

    boolean existsByWorkspaceIdAndRuleIdentifierAndSupportingMetricAndStatus(
            Long workspaceId, String ruleIdentifier, String supportingMetric, RecommendationStatus status);

    long countByWorkspaceIdAndStatus(Long workspaceId, RecommendationStatus status);

    @Query("SELECT b FROM BusinessRecommendation b WHERE b.workspace.id = :workspaceId AND b.status IN (com.sankhya.analytics.recommendations.enums.RecommendationStatus.NEW, com.sankhya.analytics.recommendations.enums.RecommendationStatus.REVIEWED) ORDER BY b.priority ASC, b.generatedAt DESC")
    List<BusinessRecommendation> findActiveByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Modifying
    @Query("DELETE FROM BusinessRecommendation b WHERE b.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
