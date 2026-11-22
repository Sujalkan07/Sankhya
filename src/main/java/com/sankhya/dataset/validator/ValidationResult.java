package com.sankhya.dataset.validator;

import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.ErrorSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidationResult {

    private final List<Map<String, Object>> validRecords = new ArrayList<>();
    private final List<ImportError> errors = new ArrayList<>();
    private int totalRows = 0;
    private int duplicateRows = 0;
    private int invalidRows = 0;
    private int validRows = 0;

    // Quality metrics (0.0 to 100.0)
    private double completenessScore = 100.0;
    private double validityScore = 100.0;
    private double uniquenessScore = 100.0;
    private double consistencyScore = 100.0;
    private double referentialIntegrityScore = 100.0;
    private double overallQualityScore = 100.0;

    public ValidationResult() {}

    public ValidationResult(int totalRows) {
        this.totalRows = totalRows;
    }

    public void addValidRecord(Map<String, Object> record) {
        this.validRecords.add(record);
        this.validRows++;
    }

    public void addError(ImportError error) {
        this.errors.add(error);
        if (error.getSeverity() == ErrorSeverity.CRITICAL || error.getSeverity() == ErrorSeverity.ERROR) {
            this.invalidRows++;
        }
    }

    public void incrementDuplicates() {
        this.duplicateRows++;
    }

    public void incrementReferentialIssues() {
        this.referentialIntegrityScore = Math.max(0.0, this.referentialIntegrityScore - 2.0);
    }

    public void incrementInconsistency() {
        this.consistencyScore = Math.max(0.0, this.consistencyScore - 2.0);
    }

    public void incrementInvalidRows() {
        this.invalidRows++;
    }

    public void incrementValidRows() {
        this.validRows++;
    }

    public void computeScores() {
        if (totalRows == 0) {
            this.completenessScore = 100.0;
            this.validityScore = 100.0;
            this.uniquenessScore = 100.0;
            this.consistencyScore = 100.0;
            this.referentialIntegrityScore = 100.0;
            this.overallQualityScore = 100.0;
            return;
        }

        long missingErrors = errors.stream().filter(e -> "MISSING_REQUIRED".equals(e.getErrorCode()) || "MISSING_REQUIRED_FIELD".equals(e.getErrorCode())).count();
        long invalidTypeErrors = errors.stream().filter(e -> "INVALID_TYPE".equals(e.getErrorCode()) || "OUT_OF_BOUNDS".equals(e.getErrorCode()) || "INVALID_DATE_FORMAT".equals(e.getErrorCode()) || "INVALID_NUMERIC_RANGE".equals(e.getErrorCode())).count();
        long duplicateErrors = errors.stream().filter(e -> "DUPLICATE_KEY".equals(e.getErrorCode()) || "DUPLICATE_SNAPSHOT".equals(e.getErrorCode())).count();
        long consistencyErrors = errors.stream().filter(e -> "INCONSISTENT_CALCULATION".equals(e.getErrorCode()) || "INVALID_RANGE".equals(e.getErrorCode()) || "STOCK_BALANCE_MISMATCH".equals(e.getErrorCode())).count();
        long referentialErrors = errors.stream().filter(e -> "FOREIGN_KEY_NOT_FOUND".equals(e.getErrorCode()) || "REFERENTIAL_INTEGRITY_VIOLATION".equals(e.getErrorCode())).count();

        this.completenessScore = Math.max(0.0, 100.0 - ((double) missingErrors / totalRows * 100.0));
        this.validityScore = Math.max(0.0, 100.0 - ((double) invalidTypeErrors / totalRows * 100.0));
        this.uniquenessScore = Math.max(0.0, 100.0 - ((double) duplicateErrors / totalRows * 100.0));
        this.consistencyScore = Math.max(0.0, 100.0 - ((double) consistencyErrors / totalRows * 100.0));
        this.referentialIntegrityScore = Math.max(0.0, 100.0 - ((double) referentialErrors / totalRows * 100.0));

        this.overallQualityScore = Math.round(((completenessScore + validityScore + uniquenessScore + consistencyScore + referentialIntegrityScore) / 5.0) * 10.0) / 10.0;
    }

    public List<Map<String, Object>> getValidRecords() {
        return validRecords;
    }

    public List<ImportError> getErrors() {
        return errors;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidRowsCount() {
        return validRows > 0 ? validRows : validRecords.size();
    }

    public int getValidRows() {
        return getValidRowsCount();
    }

    public void setValidRows(int validRows) {
        this.validRows = validRows;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(int invalidRows) {
        this.invalidRows = invalidRows;
    }

    public double getCompletenessScore() {
        return Math.round(completenessScore * 10.0) / 10.0;
    }

    public double getValidityScore() {
        return Math.round(validityScore * 10.0) / 10.0;
    }

    public double getUniquenessScore() {
        return Math.round(uniquenessScore * 10.0) / 10.0;
    }

    public double getConsistencyScore() {
        return Math.round(consistencyScore * 10.0) / 10.0;
    }

    public double getReferentialIntegrityScore() {
        return Math.round(referentialIntegrityScore * 10.0) / 10.0;
    }

    public double getReferentialScore() {
        return getReferentialIntegrityScore();
    }

    public double getOverallQualityScore() {
        return overallQualityScore;
    }

    public double getQualityScore() {
        return getOverallQualityScore();
    }
}
