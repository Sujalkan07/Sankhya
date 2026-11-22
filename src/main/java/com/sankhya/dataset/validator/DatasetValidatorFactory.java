package com.sankhya.dataset.validator;

import com.sankhya.dataset.enums.DatasetType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatasetValidatorFactory {

    private final List<DatasetValidator> validators;

    public DatasetValidatorFactory(List<DatasetValidator> validators) {
        this.validators = validators;
    }

    public DatasetValidator getValidator(DatasetType datasetType) {
        return validators.stream()
                .filter(v -> v.supports(datasetType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No validator registered for dataset type: " + datasetType));
    }
}
