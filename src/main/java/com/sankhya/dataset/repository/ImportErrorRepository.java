package com.sankhya.dataset.repository;

import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.ErrorSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportErrorRepository extends JpaRepository<ImportError, Long> {

    List<ImportError> findByDatasetImportIdOrderByRowNumberAsc(Long datasetImportId);

    Page<ImportError> findByDatasetImportIdOrderByRowNumberAsc(Long datasetImportId, Pageable pageable);

    List<ImportError> findByDatasetImportIdAndSeverityOrderByRowNumberAsc(Long datasetImportId, ErrorSeverity severity);

    long countByDatasetImportId(Long datasetImportId);

    long countByDatasetImportIdAndSeverity(Long datasetImportId, ErrorSeverity severity);

    void deleteByDatasetImportId(Long datasetImportId);
}
