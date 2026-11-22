package com.sankhya.dataset.validator;

import com.sankhya.dataset.cleaner.DataCleaningUtils;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ErrorSeverity;
import com.sankhya.products.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class InventoryDatasetValidator implements DatasetValidator {

    private final ProductRepository productRepository;

    public InventoryDatasetValidator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public boolean supports(DatasetType datasetType) {
        return datasetType == DatasetType.INVENTORY_STOCK;
    }

    @Override
    public ValidationResult validate(List<Map<String, String>> rawRows,
                                     Map<String, String> columnMapping,
                                     DatasetImport datasetImport,
                                     Long workspaceId) {
        ValidationResult result = new ValidationResult();
        result.setTotalRows(rawRows.size());

        Set<String> knownProductSkus = productRepository.findAllProductIdsByWorkspaceId(workspaceId);
        Set<String> seenDateProductPairs = new HashSet<>();

        // Invert mapping: canonical field -> file header
        Map<String, String> canonicalToFileHeader = new HashMap<>();
        columnMapping.forEach((header, canonical) -> canonicalToFileHeader.put(canonical, header));

        int rowNum = 1;
        for (Map<String, String> row : rawRows) {
            rowNum++;
            boolean rowHasErrors = false;

            String rawInvId = getVal(row, canonicalToFileHeader, "inventory_id");
            String rawDate = getVal(row, canonicalToFileHeader, "snapshot_date");
            String rawProductId = getVal(row, canonicalToFileHeader, "product_id");
            String rawOpening = getVal(row, canonicalToFileHeader, "opening_stock");
            String rawClosing = getVal(row, canonicalToFileHeader, "closing_stock");
            String rawReceived = getVal(row, canonicalToFileHeader, "received_stock");
            if (rawReceived == null) rawReceived = getVal(row, canonicalToFileHeader, "stock_received");
            String rawSold = getVal(row, canonicalToFileHeader, "sold_quantity");
            if (rawSold == null) rawSold = getVal(row, canonicalToFileHeader, "stock_sold");
            String rawCost = getVal(row, canonicalToFileHeader, "unit_cost");
            String rawReorder = getVal(row, canonicalToFileHeader, "reorder_level");
            String rawLoc = getVal(row, canonicalToFileHeader, "warehouse_or_store");

            // 1. Mandatory snapshot_date
            LocalDate snapshotDate = null;
            if (rawDate == null || rawDate.isBlank()) {
                result.addError(new ImportError(datasetImport, rowNum, "snapshot_date", rawDate,
                        "MISSING_REQUIRED", "Snapshot date is required.", ErrorSeverity.CRITICAL,
                        "Provide a valid date (YYYY-MM-DD)."));
                rowHasErrors = true;
            } else {
                snapshotDate = DataCleaningUtils.cleanDate(rawDate);
                if (snapshotDate == null) {
                    result.addError(new ImportError(datasetImport, rowNum, "snapshot_date", rawDate,
                            "INVALID_TYPE", "Invalid date format: '" + rawDate + "'.", ErrorSeverity.CRITICAL,
                            "Use YYYY-MM-DD or DD/MM/YYYY format."));
                    rowHasErrors = true;
                }
            }

            // 2. Mandatory product_id
            String productId = DataCleaningUtils.cleanText(rawProductId);
            if (productId == null) {
                result.addError(new ImportError(datasetImport, rowNum, "product_id", rawProductId,
                        "MISSING_REQUIRED", "Product SKU / ID is required.", ErrorSeverity.CRITICAL,
                        "Provide a valid product SKU."));
                rowHasErrors = true;
            } else {
                // Referential integrity check against product catalog
                if (!knownProductSkus.isEmpty() && !knownProductSkus.contains(productId)) {
                    result.addError(new ImportError(datasetImport, rowNum, "product_id", productId,
                            "FOREIGN_KEY_NOT_FOUND", "Product SKU '" + productId + "' not found in workspace product catalog.",
                            ErrorSeverity.WARNING, "Verify product SKU exists in Product Catalog or import catalog first."));
                    result.incrementReferentialIssues();
                }

                // Check uniqueness of snapshot date + product SKU pair
                if (snapshotDate != null) {
                    String pairKey = snapshotDate + "_" + productId;
                    if (seenDateProductPairs.contains(pairKey)) {
                        result.addError(new ImportError(datasetImport, rowNum, "product_id", productId,
                                "DUPLICATE_KEY", "Duplicate inventory snapshot for product '" + productId + "' on date " + snapshotDate + ".",
                                ErrorSeverity.WARNING, "Only one inventory snapshot per product per date is expected."));
                        result.incrementDuplicates();
                    } else {
                        seenDateProductPairs.add(pairKey);
                    }
                }
            }

            // 3. Opening Stock
            Integer openingStock = null;
            if (rawOpening != null && !rawOpening.isBlank()) {
                openingStock = DataCleaningUtils.cleanInteger(rawOpening);
                if (openingStock == null || openingStock < 0) {
                    result.addError(new ImportError(datasetImport, rowNum, "opening_stock", rawOpening,
                            "OUT_OF_BOUNDS", "Opening stock must be a non-negative integer.", ErrorSeverity.CRITICAL,
                            "Provide an integer >= 0."));
                    rowHasErrors = true;
                }
            }

            // 4. Stock Received
            Integer stockReceived = null;
            if (rawReceived != null && !rawReceived.isBlank()) {
                stockReceived = DataCleaningUtils.cleanInteger(rawReceived);
                if (stockReceived == null || stockReceived < 0) {
                    result.addError(new ImportError(datasetImport, rowNum, "received_stock", rawReceived,
                            "OUT_OF_BOUNDS", "Stock received must be a non-negative integer.", ErrorSeverity.CRITICAL,
                            "Provide an integer >= 0."));
                    rowHasErrors = true;
                }
            }

            // 5. Stock Sold
            Integer stockSold = null;
            if (rawSold != null && !rawSold.isBlank()) {
                stockSold = DataCleaningUtils.cleanInteger(rawSold);
                if (stockSold == null || stockSold < 0) {
                    result.addError(new ImportError(datasetImport, rowNum, "sold_quantity", rawSold,
                            "OUT_OF_BOUNDS", "Stock sold must be a non-negative integer.", ErrorSeverity.CRITICAL,
                            "Provide an integer >= 0."));
                    rowHasErrors = true;
                }
            }

            // 6. Mandatory Closing Stock
            Integer closingStock = null;
            if (rawClosing == null || rawClosing.isBlank()) {
                result.addError(new ImportError(datasetImport, rowNum, "closing_stock", rawClosing,
                        "MISSING_REQUIRED", "Closing stock is required.", ErrorSeverity.CRITICAL,
                        "Provide an integer >= 0."));
                rowHasErrors = true;
            } else {
                closingStock = DataCleaningUtils.cleanInteger(rawClosing);
                if (closingStock == null || closingStock < 0) {
                    result.addError(new ImportError(datasetImport, rowNum, "closing_stock", rawClosing,
                            "OUT_OF_BOUNDS", "Closing stock must be a non-negative integer.", ErrorSeverity.CRITICAL,
                            "Provide an integer >= 0."));
                    rowHasErrors = true;
                }
            }

            // 7. Consistency check: opening + received - sold = closing
            if (openingStock != null && stockReceived != null && stockSold != null && closingStock != null) {
                int expectedClosing = openingStock + stockReceived - stockSold;
                if (expectedClosing != closingStock) {
                    result.addError(new ImportError(datasetImport, rowNum, "closing_stock", String.valueOf(closingStock),
                            "INCONSISTENT_CALCULATION",
                            String.format("Stock balance mismatch: opening (%d) + received (%d) - sold (%d) = %d, but closing is %d.",
                                    openingStock, stockReceived, stockSold, expectedClosing, closingStock),
                            ErrorSeverity.WARNING,
                            "Verify opening, received, sold, and closing stock quantities for this snapshot."));
                    result.incrementInconsistency();
                }
            }

            // 8. Unit Cost (optional)
            if (rawCost != null && !rawCost.isBlank()) {
                BigDecimal unitCost = DataCleaningUtils.cleanDecimal(rawCost);
                if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) < 0) {
                    result.addError(new ImportError(datasetImport, rowNum, "unit_cost", rawCost,
                            "OUT_OF_BOUNDS", "Unit cost must be a non-negative number.", ErrorSeverity.WARNING,
                            "Provide a numeric value >= 0."));
                }
            }

            // 9. Reorder Level (optional)
            if (rawReorder != null && !rawReorder.isBlank()) {
                Integer reorderLevel = DataCleaningUtils.cleanInteger(rawReorder);
                if (reorderLevel == null || reorderLevel < 0) {
                    result.addError(new ImportError(datasetImport, rowNum, "reorder_level", rawReorder,
                            "OUT_OF_BOUNDS", "Reorder level must be a non-negative integer.", ErrorSeverity.WARNING,
                            "Provide an integer >= 0."));
                }
            }

            if (!rowHasErrors) {
                Map<String, Object> validRecord = new LinkedHashMap<>();
                validRecord.put("inventory_id", rawInvId != null ? rawInvId : "INV_" + snapshotDate + "_" + productId);
                validRecord.put("snapshot_date", snapshotDate);
                validRecord.put("product_id", productId);
                validRecord.put("opening_stock", openingStock != null ? openingStock : 0);
                validRecord.put("received_stock", stockReceived != null ? stockReceived : 0);
                validRecord.put("sold_quantity", stockSold != null ? stockSold : 0);
                validRecord.put("closing_stock", closingStock);
                validRecord.put("reorder_level", rawReorder != null ? DataCleaningUtils.cleanInteger(rawReorder) : 0);
                validRecord.put("warehouse_or_store", rawLoc);
                result.addValidRecord(validRecord);
            }
        }

        result.computeScores();
        return result;
    }

    private String getVal(Map<String, String> row, Map<String, String> mapping, String canonical) {
        String header = mapping.get(canonical);
        return header != null ? row.get(header) : row.get(canonical);
    }
}
