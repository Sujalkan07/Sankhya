package com.sankhya.dataset.validator;

import com.sankhya.dataset.cleaner.DataCleaningUtils;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ErrorSeverity;
import com.sankhya.products.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class ProductDatasetValidator implements DatasetValidator {

    private final ProductRepository productRepository;

    public ProductDatasetValidator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public boolean supports(DatasetType datasetType) {
        return datasetType == DatasetType.PRODUCT_CATALOG;
    }

    @Override
    public ValidationResult validate(List<Map<String, String>> rawRows,
                                     Map<String, String> columnMapping,
                                     DatasetImport datasetImport,
                                     Long workspaceId) {
        ValidationResult result = new ValidationResult();
        result.setTotalRows(rawRows.size());

        Set<String> existingProductIds = productRepository.findAllProductIdsByWorkspaceId(workspaceId);
        Set<String> seenProductIdsInFile = new HashSet<>();

        // Invert mapping: canonical field -> file header
        Map<String, String> canonicalToFileHeader = new HashMap<>();
        columnMapping.forEach((header, canonical) -> canonicalToFileHeader.put(canonical, header));

        int rowNum = 1;
        for (Map<String, String> rawRow : rawRows) {
            rowNum++;
            boolean hasCriticalError = false;

            String rawProductId = getVal(rawRow, canonicalToFileHeader, "product_id");
            String rawProductName = getVal(rawRow, canonicalToFileHeader, "product_name");
            String rawCategory = getVal(rawRow, canonicalToFileHeader, "category");
            String rawSubcategory = getVal(rawRow, canonicalToFileHeader, "subcategory");
            String rawBrand = getVal(rawRow, canonicalToFileHeader, "brand");
            String rawCost = getVal(rawRow, canonicalToFileHeader, "unit_cost");
            String rawPrice = getVal(rawRow, canonicalToFileHeader, "selling_price");
            String rawSupplier = getVal(rawRow, canonicalToFileHeader, "supplier");
            String rawExpiry = getVal(rawRow, canonicalToFileHeader, "expiry_days");

            // 1. Product ID check
            String productId = DataCleaningUtils.cleanText(rawProductId);
            if (productId == null) {
                result.addError(new ImportError(datasetImport, rowNum, "product_id", rawProductId,
                        "MISSING_REQUIRED", "Product ID / SKU is mandatory", ErrorSeverity.CRITICAL, "Provide unique SKU code"));
                hasCriticalError = true;
            } else if (seenProductIdsInFile.contains(productId)) {
                result.incrementDuplicates();
                result.addError(new ImportError(datasetImport, rowNum, "product_id", productId,
                        "DUPLICATE_KEY", "Duplicate Product ID within file: " + productId, ErrorSeverity.CRITICAL, "Ensure SKU is unique"));
                hasCriticalError = true;
            } else {
                seenProductIdsInFile.add(productId);
                if (existingProductIds.contains(productId)) {
                    result.addError(new ImportError(datasetImport, rowNum, "product_id", productId,
                            "EXISTING_RECORD", "Product ID already exists in workspace; will update existing SKU", ErrorSeverity.WARNING, "Will overwrite"));
                }
            }

            // 2. Product Name check
            String productName = DataCleaningUtils.cleanText(rawProductName);
            if (productName == null) {
                result.addError(new ImportError(datasetImport, rowNum, "product_name", rawProductName,
                        "MISSING_REQUIRED", "Product Name is required", ErrorSeverity.CRITICAL, "Provide product title"));
                hasCriticalError = true;
            }

            // 3. Category check
            String category = DataCleaningUtils.cleanText(rawCategory);
            if (category == null) {
                result.addError(new ImportError(datasetImport, rowNum, "category", rawCategory,
                        "MISSING_REQUIRED", "Category is required", ErrorSeverity.CRITICAL, "Assign retail category"));
                hasCriticalError = true;
            }

            // 4. Unit Cost check
            BigDecimal unitCost = DataCleaningUtils.parseDecimal(rawCost);
            if (unitCost == null) {
                result.addError(new ImportError(datasetImport, rowNum, "unit_cost", rawCost,
                        "INVALID_TYPE", "Unit cost must be a valid number", ErrorSeverity.CRITICAL, "Enter positive numerical cost"));
                hasCriticalError = true;
            } else if (unitCost.compareTo(BigDecimal.ZERO) < 0) {
                result.addError(new ImportError(datasetImport, rowNum, "unit_cost", rawCost,
                        "OUT_OF_BOUNDS", "Unit cost cannot be negative", ErrorSeverity.CRITICAL, "Cost must be >= 0"));
                hasCriticalError = true;
            }

            // 5. Selling Price check
            BigDecimal sellingPrice = DataCleaningUtils.parseDecimal(rawPrice);
            if (sellingPrice == null) {
                result.addError(new ImportError(datasetImport, rowNum, "selling_price", rawPrice,
                        "INVALID_TYPE", "Selling price must be a valid number", ErrorSeverity.CRITICAL, "Enter positive numerical price"));
                hasCriticalError = true;
            } else if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
                result.addError(new ImportError(datasetImport, rowNum, "selling_price", rawPrice,
                        "OUT_OF_BOUNDS", "Selling price cannot be negative", ErrorSeverity.CRITICAL, "Price must be >= 0"));
                hasCriticalError = true;
            } else if (unitCost != null && sellingPrice.compareTo(unitCost) < 0) {
                result.addError(new ImportError(datasetImport, rowNum, "selling_price", rawPrice,
                        "LOW_MARGIN", "Selling price is lower than unit cost (negative gross margin)", ErrorSeverity.WARNING, "Verify price or discount"));
            }

            // 6. Optional Expiry Days
            Integer expiryDays = DataCleaningUtils.parseInteger(rawExpiry);
            if (rawExpiry != null && !rawExpiry.isBlank() && expiryDays == null) {
                result.addError(new ImportError(datasetImport, rowNum, "expiry_days", rawExpiry,
                        "INVALID_TYPE", "Expiry days must be an integer", ErrorSeverity.WARNING, "Enter integer days"));
            } else if (expiryDays != null && expiryDays < 0) {
                result.addError(new ImportError(datasetImport, rowNum, "expiry_days", rawExpiry,
                        "OUT_OF_BOUNDS", "Expiry days cannot be negative", ErrorSeverity.WARNING, "Use positive days or leave blank"));
                expiryDays = null;
            }

            if (!hasCriticalError) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("productId", productId);
                record.put("productName", productName);
                record.put("category", category);
                record.put("subcategory", DataCleaningUtils.cleanText(rawSubcategory));
                record.put("brand", DataCleaningUtils.cleanText(rawBrand));
                record.put("unitCost", unitCost);
                record.put("sellingPrice", sellingPrice);
                record.put("supplier", DataCleaningUtils.cleanText(rawSupplier));
                record.put("expiryDays", expiryDays);
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
