package com.sankhya.ml.repository;

import com.sankhya.ml.entity.ForecastResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForecastResultRepository extends JpaRepository<ForecastResultEntity, Long> {
    List<ForecastResultEntity> findByExecutionIdOrderByForecastDateAsc(Long executionId);
    List<ForecastResultEntity> findByWorkspaceIdOrderByForecastDateAsc(Long workspaceId);
    void deleteByExecutionId(Long executionId);
}
