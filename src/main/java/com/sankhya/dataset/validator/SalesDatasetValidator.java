package com.sankhya.dataset.validator;

import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.cleaner.DataCleaningUtils;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ErrorSeverity;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.entity.SalesTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class SalesDatasetValidator implements DatasetValidator {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public SalesDatasetValidator(ProductRepository productRepository, CustomerRepository customerRepository) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public boolean supports(DatasetType datasetType) {
        return datasetType == DatasetType.SALES_TRANSACTIONS;
    }

    @Override
    public ValidationResult validate(List<Map<String, String>> rawRows,
                                     Map<String, String> columnMapping,
                                     DatasetImport datasetImport,
                                     Long workspaceId) {
        ValidationResult result = new ValidationResult();
        result.setTotalRows(rawRows.size());

        Set<String> validProductIds = productRepository.findAllProductIdsByWorkspaceId(workspaceId);
        Set<String> validCustomerIds = customerRepository.findAllCustomerIdsByWorkspaceId(workspaceId);
        boolean hasProductsInCatalog = !validProductIds.isEmpty();
        boolean hasCustomersInRegistry = !validCustomerIds.isEmpty();

        Map<String, String> canonicalToFileHeader = new HashMap<>();
        columnMapping.forEach((header, canonical) -> canonicalToFileHeader.put(canonical, header));

        int rowNum = 1;
        for (Map<String, String> rawRow : rawRows) {
            rowNum++;
            boolean hasCriticalError = false;

            String rawTxId = getVal(rawRow, canonicalToFileHeader, "transaction_id");
            String rawDate = getVal(rawRow, canonicalToFileHeader, "transaction_date");
            String rawProductId = getVal(rawRow, canonicalToFileHeader, "product_id");
            String rawCustomerId = getVal(rawRow, canonicalToFileHeader, "customer_id");
            String rawQuantity = getVal(rawRow, canonicalToFileHeader, "quantity");
            String rawUnitPrice = getVal(rawRow, canonicalToFileHeader, "unit_price");
            String rawDiscount = getVal(rawRow, canonicalToFileHeader, "discount");
            String rawPayment = getVal(rawRow, canonicalToFileHeader, "payment_method");
            String rawStoreId = getVal(rawRow, canonicalToFileHeader, "store_id");

            // 1. Transaction ID check
            String txId = DataCleaningUtils.cleanText(rawTxId);
            if (txId == null) {
                result.addError(new ImportError(datasetImport, rowNum, "transaction_id", rawTxId,
                        "MISSING_REQUIRED", "Transaction ID / Bill Number is mandatory", ErrorSeverity.CRITICAL, "Provide receipt ID"));
                hasCriticalError = true;
            }

            // 2. Transaction Date check
            LocalDateTime txDate = DataCleaningUtils.parseDateTime(rawDate);
            if (txDate == null) {
                result.addError(new ImportError(datasetImport, rowNum, "transaction_date", rawDate,
                        "INVALID_TYPE", "Transaction date unparseable: " + rawDate, ErrorSeverity.CRITICAL, "Use format YYYY-MM-DD HH:MM:SS"));
                hasCriticalError = true;
            } else if (txDate.isAfter(LocalDateTime.now().plusDays(1))) {
                result.addError(new ImportError(datasetImport, rowNum, "transaction_date", rawDate,
                        "FUTURE_DATE", "Transaction date is in future: " + rawDate, ErrorSeverity.WARNING, "Check timezone or date"));
            }

            // 3. Product ID & Referential Integrity
            String productId = DataCleaningUtils.cleanText(rawProductId);
            if (productId == null) {
                result.addError(new ImportError(datasetImport, rowNum, "product_id", rawProductId,
                        "MISSING_REQUIRED", "Product ID / SKU is mandatory", ErrorSeverity.CRITICAL, "Provide valid product SKU"));
                hasCriticalError = true;
            } else if (hasProductsInCatalog && !validProductIds.contains(productId)) {
                result.addError(new ImportError(datasetImport, rowNum, "product_id", productId,
                        "FOREIGN_KEY_NOT_FOUND", "Product SKU not found in Product Master Catalog: " + productId,
                        ErrorSeverity.WARNING, "Import or update product master catalog first"));
            }

            // 4. Customer ID Referential Integrity (optional customer)
            String customerId = DataCleaningUtils.cleanText(rawCustomerId);
            if (customerId != null && hasCustomersInRegistry && !validCustomerIds.contains(customerId)) {
                result.addError(new ImportError(datasetImport, rowNum, "customer_id", customerId,
                        "FOREIGN_KEY_NOT_FOUND", "Customer ID not found in Customer Registry: " + customerId,
                        ErrorSeverity.WARNING, "Register customer or treat as anonymous"));
            }

            // 5. Quantity check
            Integer quantity = DataCleaningUtils.parseInteger(rawQuantity);
            if (quantity == null) {
                result.addError(new ImportError(datasetImport, rowNum, "quantity", rawQuantity,
                        "INVALID_TYPE", "Quantity must be an integer", ErrorSeverity.CRITICAL, "Enter positive whole units"));
                hasCriticalError = true;
            } else if (quantity <= 0) {
                result.addError(new ImportError(datasetImport, rowNum, "quantity", rawQuantity,
                        "OUT_OF_BOUNDS", "Quantity must be positive (> 0), got: " + quantity, ErrorSeverity.CRITICAL, "Quantity must be at least 1"));
                hasCriticalError = true;
            }

            // 6. Unit Price check
            BigDecimal unitPrice = DataCleaningUtils.parseDecimal(rawUnitPrice);
            if (unitPrice == null) {
                result.addError(new ImportError(datasetImport, rowNum, "unit_price", rawUnitPrice,
                        "INVALID_TYPE", "Unit price must be numeric", ErrorSeverity.CRITICAL, "Enter selling price in INR"));
                hasCriticalError = true;
            } else if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                result.addError(new ImportError(datasetImport, rowNum, "unit_price", rawUnitPrice,
                        "OUT_OF_BOUNDS", "Unit price cannot be negative", ErrorSeverity.CRITICAL, "Price must be >= 0"));
                hasCriticalError = true;
            }

            // 7. Discount check
            BigDecimal discount = DataCleaningUtils.parseDecimal(rawDiscount);
            if (discount == null) {
                discount = BigDecimal.ZERO; // Safe default
            } else if (discount.compareTo(BigDecimal.ZERO) < 0) {
                result.addError(new ImportError(datasetImport, rowNum, "discount", rawDiscount,
                        "OUT_OF_BOUNDS", "Discount cannot be negative", ErrorSeverity.WARNING, "Defaulted to 0.00"));
                discount = BigDecimal.ZERO;
            }

            // Check discount <= gross amount
            if (quantity != null && unitPrice != null) {
                BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
                if (discount.compareTo(gross) > 0) {
                    result.addError(new ImportError(datasetImport, rowNum, "discount", rawDiscount,
                            "INCONSISTENT_CALCULATION", "Discount (" + discount + ") exceeds line total (" + gross + ")",
                            ErrorSeverity.CRITICAL, "Discount cannot exceed line amount"));
                    hasCriticalError = true;
                }
            }

            if (!hasCriticalError && quantity != null && unitPrice != null) {
                BigDecimal revenue = SalesTransaction.calculateRevenue(quantity, unitPrice, discount);
                String normalizedPayment = DataCleaningUtils.normalizePaymentMethod(rawPayment);

                Map<String, Object> record = new LinkedHashMap<>();
                record.put("transactionId", txId);
                record.put("transactionDate", txDate);
                record.put("customerId", customerId);
                record.put("productId", productId);
                record.put("quantity", quantity);
                record.put("unitPrice", unitPrice);
                record.put("discount", discount);
                record.put("revenue", revenue);
                record.put("paymentMethod", normalizedPayment);
                record.put("storeId", DataCleaningUtils.cleanText(rawStoreId));
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
