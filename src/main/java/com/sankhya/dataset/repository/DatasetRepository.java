package com.sankhya.dataset.repository;

import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    List<Dataset> findByWorkspaceId(Long workspaceId);

    Optional<Dataset> findByIdAndWorkspaceId(Long id, Long workspaceId);

    Optional<Dataset> findByWorkspaceIdAndDatasetType(Long workspaceId, DatasetType datasetType);

    @Query("SELECT COALESCE(SUM(d.recordCount), 0) FROM Dataset d WHERE d.workspace.id = :workspaceId")
    Long sumTotalRecordsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT COUNT(d) FROM Dataset d WHERE d.workspace.id = :workspaceId AND d.recordCount > 0")
    long countPopulatedDatasetsByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
