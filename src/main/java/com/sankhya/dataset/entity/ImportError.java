package com.sankhya.dataset.entity;

import com.sankhya.dataset.enums.ErrorSeverity;
import jakarta.persistence.*;

@Entity
@Table(name = "import_errors",
        indexes = {
                @Index(name = "idx_error_import", columnList = "dataset_import_id"),
                @Index(name = "idx_error_severity", columnList = "severity")
        })
public class ImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_import_id")
    private DatasetImport datasetImport;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "column_name", length = 100)
    private String columnName;

    @Column(name = "original_value", length = 500)
    private String originalValue;

    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;

    @Column(name = "error_message", nullable = false, length = 500)
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ErrorSeverity severity = ErrorSeverity.CRITICAL;

    @Column(name = "suggested_correction", length = 500)
    private String suggestedCorrection;

    public ImportError() {}

    public ImportError(DatasetImport datasetImport, int rowNumber, String columnName,
                       String originalValue, String errorCode, String errorMessage,
                       ErrorSeverity severity, String suggestedCorrection) {
        this.datasetImport = datasetImport;
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.originalValue = originalValue;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.severity = severity;
        this.suggestedCorrection = suggestedCorrection;
    }

    public ImportError(Long rowNumber, String columnName, String originalValue,
                       String errorMessage, ErrorSeverity severity, String errorCode,
                       String suggestedCorrection) {
        this.rowNumber = rowNumber != null ? rowNumber.intValue() : 0;
        this.columnName = columnName;
        this.originalValue = originalValue;
        this.errorMessage = errorMessage;
        this.severity = severity;
        this.errorCode = errorCode;
        this.suggestedCorrection = suggestedCorrection;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public DatasetImport getDatasetImport() {
        return datasetImport;
    }

    public void setDatasetImport(DatasetImport datasetImport) {
        this.datasetImport = datasetImport;
    }

    public Long getRowNumber() {
        return (long) rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public void setRowNumber(Long rowNumber) {
        this.rowNumber = rowNumber != null ? rowNumber.intValue() : 0;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public String getInvalidValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ErrorSeverity severity) {
        this.severity = severity;
    }

    public String getSuggestedCorrection() {
        return suggestedCorrection;
    }

    public String getSuggestion() {
        return suggestedCorrection;
    }

    public void setSuggestedCorrection(String suggestedCorrection) {
        this.suggestedCorrection = suggestedCorrection;
    }
}
