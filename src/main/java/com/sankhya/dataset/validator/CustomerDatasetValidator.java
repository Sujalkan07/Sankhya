package com.sankhya.dataset.validator;

import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.cleaner.DataCleaningUtils;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ErrorSeverity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class CustomerDatasetValidator implements DatasetValidator {

    private final CustomerRepository customerRepository;

    public CustomerDatasetValidator(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public boolean supports(DatasetType datasetType) {
        return datasetType == DatasetType.CUSTOMER_REGISTRY;
    }

    @Override
    public ValidationResult validate(List<Map<String, String>> rawRows,
                                     Map<String, String> columnMapping,
                                     DatasetImport datasetImport,
                                     Long workspaceId) {
        ValidationResult result = new ValidationResult();
        result.setTotalRows(rawRows.size());

        Set<String> existingCustomerIds = customerRepository.findAllCustomerIdsByWorkspaceId(workspaceId);
        Set<String> seenInFile = new HashSet<>();

        Map<String, String> canonicalToFileHeader = new HashMap<>();
        columnMapping.forEach((header, canonical) -> canonicalToFileHeader.put(canonical, header));

        int rowNum = 1;
        for (Map<String, String> rawRow : rawRows) {
            rowNum++;
            boolean hasCriticalError = false;

            String rawCustomerId = getVal(rawRow, canonicalToFileHeader, "customer_id");
            String rawCustomerName = getVal(rawRow, canonicalToFileHeader, "customer_name");
            String rawGender = getVal(rawRow, canonicalToFileHeader, "gender");
            String rawAge = getVal(rawRow, canonicalToFileHeader, "age");
            String rawCity = getVal(rawRow, canonicalToFileHeader, "city");
            String rawRegDate = getVal(rawRow, canonicalToFileHeader, "registration_date");
            String rawSegment = getVal(rawRow, canonicalToFileHeader, "customer_segment");

            // 1. Customer ID check
            String customerId = DataCleaningUtils.cleanText(rawCustomerId);
            if (customerId == null) {
                result.addError(new ImportError(datasetImport, rowNum, "customer_id", rawCustomerId,
                        "MISSING_REQUIRED", "Customer ID is mandatory", ErrorSeverity.CRITICAL, "Provide unique customer ID"));
                hasCriticalError = true;
            } else if (seenInFile.contains(customerId)) {
                result.incrementDuplicates();
                result.addError(new ImportError(datasetImport, rowNum, "customer_id", customerId,
                        "DUPLICATE_KEY", "Duplicate Customer ID in file: " + customerId, ErrorSeverity.CRITICAL, "Remove duplicate"));
                hasCriticalError = true;
            } else {
                seenInFile.add(customerId);
                if (existingCustomerIds.contains(customerId)) {
                    result.addError(new ImportError(datasetImport, rowNum, "customer_id", customerId,
                            "EXISTING_RECORD", "Customer already exists in workspace; profile will be updated", ErrorSeverity.WARNING, "Will update"));
                }
            }

            // 2. Age check (if supplied)
            Integer age = DataCleaningUtils.parseInteger(rawAge);
            if (rawAge != null && !rawAge.isBlank() && age == null) {
                result.addError(new ImportError(datasetImport, rowNum, "age", rawAge,
                        "INVALID_TYPE", "Age must be an integer", ErrorSeverity.WARNING, "Enter numeric age"));
            } else if (age != null && (age < 10 || age > 115)) {
                result.addError(new ImportError(datasetImport, rowNum, "age", rawAge,
                        "OUT_OF_BOUNDS", "Customer age is outside standard range (10-115): " + age, ErrorSeverity.WARNING, "Verify age"));
            }

            // 3. Registration date (if supplied)
            LocalDate regDate = DataCleaningUtils.parseDate(rawRegDate);
            if (rawRegDate != null && !rawRegDate.isBlank() && regDate == null) {
                result.addError(new ImportError(datasetImport, rowNum, "registration_date", rawRegDate,
                        "INVALID_TYPE", "Registration date format unparseable: " + rawRegDate, ErrorSeverity.WARNING, "Use YYYY-MM-DD"));
            } else if (regDate != null && regDate.isAfter(LocalDate.now())) {
                result.addError(new ImportError(datasetImport, rowNum, "registration_date", rawRegDate,
                        "FUTURE_DATE", "Registration date cannot be in the future", ErrorSeverity.WARNING, "Use current/past date"));
            }

            if (!hasCriticalError) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("customerId", customerId);
                record.put("customerName", DataCleaningUtils.cleanText(rawCustomerName));
                record.put("gender", DataCleaningUtils.cleanText(rawGender));
                record.put("age", age);
                record.put("city", DataCleaningUtils.cleanText(rawCity));
                record.put("registrationDate", regDate);
                record.put("customerSegment", DataCleaningUtils.cleanText(rawSegment));
                result.addValidRecord(record);
            }
        }

        result.computeScores();
        return result;
    }

    private String getVal(Map<String, String> row, Map<String, String> canonicalToFileHeader, String canonical) {
        String header = canonicalToFileHeader.get(canonical);
        return header != null ? row.get(header) : null;
    }
}
