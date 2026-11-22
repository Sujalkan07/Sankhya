package com.sankhya.dataset.repository;

import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ImportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatasetImportRepository extends JpaRepository<DatasetImport, Long> {

    List<DatasetImport> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    Page<DatasetImport> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId, Pageable pageable);

    List<DatasetImport> findByWorkspaceIdAndDatasetTypeOrderByCreatedAtDesc(Long workspaceId, DatasetType datasetType);

    Optional<DatasetImport> findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Query("SELECT COUNT(i) FROM DatasetImport i WHERE i.workspace.id = :workspaceId AND i.status IN (:statuses)")
    long countByWorkspaceIdAndStatusIn(@Param("workspaceId") Long workspaceId, @Param("statuses") List<ImportStatus> statuses);

    @Query("SELECT COUNT(i) FROM DatasetImport i WHERE i.workspace.id = :workspaceId AND i.status = :status")
    long countByWorkspaceIdAndStatus(@Param("workspaceId") Long workspaceId, @Param("status") ImportStatus status);

    List<DatasetImport> findTop5ByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    List<DatasetImport> findTop10ByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
}
