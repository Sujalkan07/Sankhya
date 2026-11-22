package com.sankhya.dataset.validator;

import com.sankhya.dataset.cleaner.DataCleaningUtils;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ErrorSeverity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class UnifiedMartValidator implements DatasetValidator {

    @Override
    public boolean supports(DatasetType datasetType) {
        return datasetType == DatasetType.UNIFIED_MART;
    }

    @Override
    public ValidationResult validate(List<Map<String, String>> rawRows,
                                     Map<String, String> columnMapping,
                                     DatasetImport datasetImport,
                                     Long workspaceId) {
        ValidationResult result = new ValidationResult();
        result.setTotalRows(rawRows.size());

        Map<String, String> canonicalToFileHeader = new HashMap<>();
        columnMapping.forEach((header, canonical) -> canonicalToFileHeader.put(canonical, header));

        // Provenance & demo data info warning
        result.addError(new ImportError(
                datasetImport,
                1,
                "dataset_provenance",
                "Instacart / Synthetic",
                "DATASET_PROVENANCE_DISCLOSURE",
                "Demonstration mart contains Instacart-derived basket topology, reconstructed calendar dates, and synthetic customer, pricing, and inventory attributes.",
                ErrorSeverity.WARNING,
                "Analytics reflect synthetic demonstration modeling."
        ));

        // Tracking for batch consistency
        Map<String, String[]> receiptConsistencyMap = new HashMap<>(); // txId -> [dateStr, customerId, paymentMethod, cashier]
        Map<String, String[]> productConsistencyMap = new HashMap<>(); // prodId -> [name, category, brand, cost, price]
        Map<String, String[]> customerConsistencyMap = new HashMap<>(); // custId -> [name, city, segment]
        Map<String, String[]> inventoryConsistencyMap = new HashMap<>(); // prodId -> [snapshotDate, closingStock, reorderLevel, status]

        int validCount = 0;
        int errorCount = 0;
        int maxErrorsRecorded = 500; // Cap detailed errors to protect memory

        int rowNum = 1;
        for (Map<String, String> rawRow : rawRows) {
            rowNum++;
            boolean hasCriticalError = false;

            // 1. Core Receipt fields
            String txId = getVal(rawRow, canonicalToFileHeader, "transaction_id");
            String txDateRaw = getVal(rawRow, canonicalToFileHeader, "transaction_date");
            String paymentMethod = getVal(rawRow, canonicalToFileHeader, "payment_method");
            String cashierName = getVal(rawRow, canonicalToFileHeader, "cashier_name");

            if (txId == null || txId.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum, "transaction_id", "",
                            "REQUIRED_FIELD_MISSING", "transaction_id is required", ErrorSeverity.CRITICAL, "Provide receipt identifier"));
                }
                hasCriticalError = true;
            }

            LocalDateTime txDate = null;
            if (txDateRaw == null || txDateRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum, "transaction_date", "",
                            "REQUIRED_FIELD_MISSING", "transaction_date is required", ErrorSeverity.CRITICAL, "Provide purchase timestamp"));
                }
                hasCriticalError = true;
            } else {
                txDate = DataCleaningUtils.cleanDateTime(txDateRaw);
                if (txDate == null) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "transaction_date", txDateRaw,
                                "INVALID_DATETIME_FORMAT", "Failed to parse purchase timestamp", ErrorSeverity.CRITICAL, "Use format YYYY-MM-DD HH:mm:ss"));
                    }
                    hasCriticalError = true;
                }
            }

            // Receipt consistency rule (lines sharing transaction_id must share date, customer, cashier, tender)
            if (txId != null && !txId.isBlank()) {
                String custIdForReceipt = getVal(rawRow, canonicalToFileHeader, "customer_id");
                String[] existing = receiptConsistencyMap.get(txId);
                if (existing == null) {
                    receiptConsistencyMap.put(txId, new String[]{txDateRaw != null ? txDateRaw.trim() : "",
                            custIdForReceipt != null ? custIdForReceipt.trim() : "",
                            paymentMethod != null ? paymentMethod.trim() : "",
                            cashierName != null ? cashierName.trim() : ""});
                } else {
                    if (txDateRaw != null && !txDateRaw.trim().equalsIgnoreCase(existing[0])) {
                        if (result.getErrors().size() < maxErrorsRecorded) {
                            result.addError(new ImportError(datasetImport, rowNum, "transaction_date", txDateRaw,
                                    "RECEIPT_LINE_INCONSISTENCY", "Transaction date mismatch with earlier line items in receipt " + txId, ErrorSeverity.CRITICAL, "Ensure consistent receipt timestamps"));
                        }
                        hasCriticalError = true;
                    }
                }
            }

            // 2. Customer fields
            String customerId = getVal(rawRow, canonicalToFileHeader, "customer_id");
            String customerName = getVal(rawRow, canonicalToFileHeader, "customer_name");
            String city = getVal(rawRow, canonicalToFileHeader, "city");
            String segment = getVal(rawRow, canonicalToFileHeader, "customer_segment");

            if (customerId != null && !customerId.isBlank()) {
                String[] existingCust = customerConsistencyMap.get(customerId.trim());
                if (existingCust == null) {
                    customerConsistencyMap.put(customerId.trim(), new String[]{
                            customerName != null ? customerName.trim() : "",
                            city != null ? city.trim() : "",
                            segment != null ? segment.trim() : ""
                    });
                } else if (customerName != null && !customerName.isBlank() && !customerName.trim().equalsIgnoreCase(existingCust[0])) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "customer_name", customerName,
                                "CUSTOMER_NAME_INCONSISTENCY", "Customer ID maps to multiple names: " + existingCust[0] + " vs " + customerName, ErrorSeverity.WARNING, "Standardize customer name"));
                    }
                }
            }

            // 3. Product fields
            String productId = getVal(rawRow, canonicalToFileHeader, "product_id");
            String productName = getVal(rawRow, canonicalToFileHeader, "product_name");
            String category = getVal(rawRow, canonicalToFileHeader, "category");
            String subcategory = getVal(rawRow, canonicalToFileHeader, "subcategory");
            String brand = getVal(rawRow, canonicalToFileHeader, "brand");
            String unitCostRaw = getVal(rawRow, canonicalToFileHeader, "unit_cost");
            String sellingPriceRaw = getVal(rawRow, canonicalToFileHeader, "selling_price");

            if (productId == null || productId.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum, "product_id", "",
                            "REQUIRED_FIELD_MISSING", "product_id is required", ErrorSeverity.CRITICAL, "Provide SKU / barcode"));
                }
                hasCriticalError = true;
            }

            if (productName == null || productName.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum, "product_name", "",
                            "REQUIRED_FIELD_MISSING", "product_name is required", ErrorSeverity.CRITICAL, "Provide product name"));
                }
                hasCriticalError = true;
            }

            BigDecimal unitCost = null;
            if (unitCostRaw == null || unitCostRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum, "unit_cost", "",
                            "REQUIRED_FIELD_MISSING", "unit_cost is required", ErrorSeverity.CRITICAL, "Provide wholesale cost"));
                }
                hasCriticalError = true;
            } else {
                unitCost = DataCleaningUtils.cleanDecimal(unitCostRaw);
                if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "unit_cost", unitCostRaw,
                                "INVALID_NUMERIC_BOUNDS", "unit_cost must be a non-negative decimal", ErrorSeverity.CRITICAL, "Provide non-negative value"));
                    }
                    hasCriticalError = true;
                }
            }

            BigDecimal sellingPrice = null;
            if (sellingPriceRaw == null || sellingPriceRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum, "selling_price", "",
                            "REQUIRED_FIELD_MISSING", "selling_price is required", ErrorSeverity.CRITICAL, "Provide selling price"));
                }
                hasCriticalError = true;
            } else {
                sellingPrice = DataCleaningUtils.cleanDecimal(sellingPriceRaw);
                if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "selling_price", sellingPriceRaw,
                                "INVALID_NUMERIC_BOUNDS", "selling_price must be a non-negative decimal", ErrorSeverity.CRITICAL, "Provide non-negative value"));
                    }
                    hasCriticalError = true;
                }
            }

            // 4. Line item metrics
            String qtyRaw = getVal(rawRow, canonicalToFileHeader, "quantity");
            String unitPriceRaw = getVal(rawRow, canonicalToFileHeader, "unit_price");
            String discountRaw = getVal(rawRow, canonicalToFileHeader, "discount");

            Integer quantity = null;
            if (qtyRaw == null || qtyRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum, "quantity", "",
                            "REQUIRED_FIELD_MISSING", "quantity is required", ErrorSeverity.CRITICAL, "Provide integer > 0"));
                }
                hasCriticalError = true;
            } else {
                quantity = DataCleaningUtils.cleanInteger(qtyRaw);
                if (quantity == null || quantity <= 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "quantity", qtyRaw,
                                "INVALID_QUANTITY", "quantity must be an integer > 0", ErrorSeverity.CRITICAL, "Ensure quantity is positive"));
                    }
                    hasCriticalError = true;
                }
            }

            BigDecimal unitPrice = null;
            if (unitPriceRaw == null || unitPriceRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum, "unit_price", "",
                            "REQUIRED_FIELD_MISSING", "unit_price is required", ErrorSeverity.CRITICAL, "Provide unit selling price"));
                }
                hasCriticalError = true;
            } else {
                unitPrice = DataCleaningUtils.cleanDecimal(unitPriceRaw);
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "unit_price", unitPriceRaw,
                                "INVALID_NUMERIC_BOUNDS", "unit_price must be non-negative", ErrorSeverity.CRITICAL, "Provide non-negative price"));
                    }
                    hasCriticalError = true;
                }
            }

            BigDecimal discount = BigDecimal.ZERO;
            if (discountRaw != null && !discountRaw.isBlank()) {
                BigDecimal d = DataCleaningUtils.cleanDecimal(discountRaw);
                if (d != null) {
                    if (d.compareTo(BigDecimal.ZERO) < 0) {
                        if (result.getErrors().size() < maxErrorsRecorded) {
                            result.addError(new ImportError(datasetImport, rowNum, "discount", discountRaw,
                                    "INVALID_NUMERIC_BOUNDS", "discount cannot be negative", ErrorSeverity.CRITICAL, "Set discount >= 0"));
                        }
                        hasCriticalError = true;
                    } else {
                        discount = d;
                    }
                }
            }

            if (quantity != null && unitPrice != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal maxDiscount = unitPrice.multiply(BigDecimal.valueOf(quantity));
                if (discount.compareTo(maxDiscount) > 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "discount", discountRaw,
                                "DISCOUNT_EXCEEDS_GROSS", "discount exceeds line gross value (" + maxDiscount + ")", ErrorSeverity.CRITICAL, "Clamp discount to line value"));
                    }
                    hasCriticalError = true;
                }
            }

            // 5. Inventory context bounds
            String stockRaw = getVal(rawRow, canonicalToFileHeader, "current_closing_stock");
            if (stockRaw != null && !stockRaw.isBlank()) {
                Integer stock = DataCleaningUtils.cleanInteger(stockRaw);
                if (stock != null && stock < 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "current_closing_stock", stockRaw,
                                "INVALID_NUMERIC_BOUNDS", "current_closing_stock cannot be negative", ErrorSeverity.CRITICAL, "Provide non-negative stock count"));
                    }
                    hasCriticalError = true;
                }
            }

            String reorderRaw = getVal(rawRow, canonicalToFileHeader, "current_reorder_level");
            if (reorderRaw != null && !reorderRaw.isBlank()) {
                Integer reorder = DataCleaningUtils.cleanInteger(reorderRaw);
                if (reorder != null && reorder < 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum, "current_reorder_level", reorderRaw,
                                "INVALID_NUMERIC_BOUNDS", "current_reorder_level cannot be negative", ErrorSeverity.CRITICAL, "Provide non-negative reorder level"));
                    }
                    hasCriticalError = true;
                }
            }

            if (hasCriticalError) {
                errorCount++;
            } else {
                validCount++;
            }
        }

        result.setValidRows(validCount);
        result.setInvalidRows(errorCount);
        return result;
    }

    public ValidationResult validateStreaming(java.io.File file, com.sankhya.dataset.parser.FileParser parser,
                                              Map<String, String> columnMapping,
                                              DatasetImport datasetImport,
                                              Long workspaceId) throws java.io.IOException {
        ValidationResult result = new ValidationResult();

        Map<String, String> canonicalToFileHeader = new HashMap<>();
        columnMapping.forEach((header, canonical) -> canonicalToFileHeader.put(canonical, header));

        // Provenance & demo data info warning
        result.addError(new ImportError(
                datasetImport,
                1,
                "dataset_provenance",
                "Instacart / Synthetic",
                "DATASET_PROVENANCE_DISCLOSURE",
                "Demonstration mart contains Instacart-derived basket topology, reconstructed calendar dates, and synthetic customer, pricing, and inventory attributes.",
                ErrorSeverity.WARNING,
                "Analytics reflect synthetic demonstration modeling."
        ));

        int[] counts = new int[3]; // [total, valid, invalid]
        final int maxErrorsRecorded = 200;

        parser.processRowsStreaming(file, (rowNum, rawRow) -> {
            counts[0]++;
            boolean hasCriticalError = false;

            String txId = getVal(rawRow, canonicalToFileHeader, "transaction_id");
            String txDateRaw = getVal(rawRow, canonicalToFileHeader, "transaction_date");

            if (txId == null || txId.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum.intValue(), "transaction_id", "",
                            "REQUIRED_FIELD_MISSING", "transaction_id is required", ErrorSeverity.CRITICAL, "Provide receipt identifier"));
                }
                hasCriticalError = true;
            }

            if (txDateRaw == null || txDateRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum.intValue(), "transaction_date", "",
                            "REQUIRED_FIELD_MISSING", "transaction_date is required", ErrorSeverity.CRITICAL, "Provide purchase timestamp"));
                }
                hasCriticalError = true;
            } else {
                LocalDateTime txDate = DataCleaningUtils.cleanDateTime(txDateRaw);
                if (txDate == null) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum.intValue(), "transaction_date", txDateRaw,
                                "INVALID_DATETIME_FORMAT", "Failed to parse purchase timestamp", ErrorSeverity.CRITICAL, "Use format YYYY-MM-DD HH:mm:ss"));
                    }
                    hasCriticalError = true;
                }
            }

            String productId = getVal(rawRow, canonicalToFileHeader, "product_id");
            String productName = getVal(rawRow, canonicalToFileHeader, "product_name");
            String unitCostRaw = getVal(rawRow, canonicalToFileHeader, "unit_cost");
            String sellingPriceRaw = getVal(rawRow, canonicalToFileHeader, "selling_price");

            if (productId == null || productId.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum.intValue(), "product_id", "",
                            "REQUIRED_FIELD_MISSING", "product_id is required", ErrorSeverity.CRITICAL, "Provide SKU / barcode"));
                }
                hasCriticalError = true;
            }

            if (productName == null || productName.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum.intValue(), "product_name", "",
                            "REQUIRED_FIELD_MISSING", "product_name is required", ErrorSeverity.CRITICAL, "Provide product name"));
                }
                hasCriticalError = true;
            }

            if (unitCostRaw == null || unitCostRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum.intValue(), "unit_cost", "",
                            "REQUIRED_FIELD_MISSING", "unit_cost is required", ErrorSeverity.CRITICAL, "Provide wholesale cost"));
                }
                hasCriticalError = true;
            }

            if (sellingPriceRaw == null || sellingPriceRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum.intValue(), "selling_price", "",
                            "REQUIRED_FIELD_MISSING", "selling_price is required", ErrorSeverity.CRITICAL, "Provide selling price"));
                }
                hasCriticalError = true;
            }

            String qtyRaw = getVal(rawRow, canonicalToFileHeader, "quantity");
            String unitPriceRaw = getVal(rawRow, canonicalToFileHeader, "unit_price");

            if (qtyRaw == null || qtyRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum.intValue(), "quantity", "",
                            "REQUIRED_FIELD_MISSING", "quantity is required", ErrorSeverity.CRITICAL, "Provide integer > 0"));
                }
                hasCriticalError = true;
            } else {
                Integer quantity = DataCleaningUtils.cleanInteger(qtyRaw);
                if (quantity == null || quantity <= 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum.intValue(), "quantity", qtyRaw,
                                "INVALID_QUANTITY", "quantity must be an integer > 0", ErrorSeverity.CRITICAL, "Ensure quantity is positive"));
                    }
                    hasCriticalError = true;
                }
            }

            if (unitPriceRaw == null || unitPriceRaw.isBlank()) {
                if (result.getErrors().size() < maxErrorsRecorded) {
                    result.addError(new ImportError(datasetImport, rowNum.intValue(), "unit_price", "",
                            "REQUIRED_FIELD_MISSING", "unit_price is required", ErrorSeverity.CRITICAL, "Provide unit selling price"));
                }
                hasCriticalError = true;
            } else {
                BigDecimal unitPrice = DataCleaningUtils.cleanDecimal(unitPriceRaw);
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                    if (result.getErrors().size() < maxErrorsRecorded) {
                        result.addError(new ImportError(datasetImport, rowNum.intValue(), "unit_price", unitPriceRaw,
                                "INVALID_NUMERIC_BOUNDS", "unit_price must be non-negative", ErrorSeverity.CRITICAL, "Provide non-negative price"));
                    }
                    hasCriticalError = true;
                }
            }

            if (hasCriticalError) {
                counts[2]++;
            } else {
                counts[1]++;
            }
        });

        result.setTotalRows(counts[0]);
        result.setValidRows(counts[1]);
        result.setInvalidRows(counts[2]);
        return result;
    }

    private String getVal(Map<String, String> row, Map<String, String> mapping, String canonicalName) {
        String fileHeader = mapping.get(canonicalName);
        if (fileHeader == null) {
            // direct lookup
            return row.get(canonicalName);
        }
        return row.get(fileHeader);
    }
}
