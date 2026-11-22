package com.sankhya.dataset.validator;

import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.enums.DatasetType;

import java.util.List;
import java.util.Map;

public interface DatasetValidator {

    boolean supports(DatasetType datasetType);

    ValidationResult validate(List<Map<String, String>> rawRows,
                             Map<String, String> columnMapping,
                             DatasetImport datasetImport,
                             Long workspaceId);
}
