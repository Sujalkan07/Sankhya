package com.sankhya.dataset.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankhya.customers.entity.Customer;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.cleaner.DataCleaningUtils;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.entity.DatasetImport;
import com.sankhya.dataset.entity.ImportError;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.enums.ErrorSeverity;
import com.sankhya.dataset.enums.ImportStatus;
import com.sankhya.dataset.parser.FileParser;
import com.sankhya.dataset.parser.FileParserFactory;
import com.sankhya.dataset.parser.ParsedFileData;
import com.sankhya.dataset.repository.DatasetImportRepository;
import com.sankhya.dataset.repository.DatasetRepository;
import com.sankhya.dataset.repository.ImportErrorRepository;
import com.sankhya.dataset.schema.DatasetSchemaDefinition;
import com.sankhya.dataset.validator.DatasetValidator;
import com.sankhya.dataset.validator.DatasetValidatorFactory;
import com.sankhya.dataset.validator.ValidationResult;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.entity.SalesTransaction;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.workspace.entity.Workspace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DatasetImportService {

    private final DatasetImportRepository importRepository;
    private final DatasetRepository datasetRepository;
    private final ImportErrorRepository errorRepository;
    private final FileStorageService storageService;
    private final FileParserFactory parserFactory;
    private final DatasetValidatorFactory validatorFactory;
    private final DatasetService datasetService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final ObjectMapper objectMapper;

    public DatasetImportService(DatasetImportRepository importRepository,
                                DatasetRepository datasetRepository,
                                ImportErrorRepository errorRepository,
                                FileStorageService storageService,
                                FileParserFactory parserFactory,
                                DatasetValidatorFactory validatorFactory,
                                DatasetService datasetService,
                                ProductRepository productRepository,
                                CustomerRepository customerRepository,
                                SalesTransactionRepository salesTransactionRepository,
                                InventorySnapshotRepository inventorySnapshotRepository,
                                ObjectMapper objectMapper) {
        this.importRepository = importRepository;
        this.datasetRepository = datasetRepository;
        this.errorRepository = errorRepository;
        this.storageService = storageService;
        this.parserFactory = parserFactory;
        this.validatorFactory = validatorFactory;
        this.datasetService = datasetService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DatasetImport stageUpload(Workspace workspace, DatasetType datasetType, MultipartFile file) throws IOException {
        String storedPath = storageService.storeFile(workspace.getId(), file);

        Dataset dataset = datasetService.findOrCreateDataset(workspace, datasetType);

        DatasetImport datasetImport = new DatasetImport(
                workspace,
                dataset,
                datasetType,
                file.getOriginalFilename(),
                storedPath,
                file.getSize()
        );

        // Parse preview rows to detect headers & auto-map
        File diskFile = storageService.getFile(storedPath);
        FileParser parser = parserFactory.getParser(file.getOriginalFilename());
        ParsedFileData previewData = parser.readPreviewAndHeaders(diskFile, 15);

        datasetImport.setTotalRows(previewData.getTotalRowCount());

        Map<String, String> detectedMapping = DatasetSchemaDefinition.autoDetectMappings(datasetType, previewData.getHeaders());
        try {
            datasetImport.setColumnMappingJson(objectMapper.writeValueAsString(detectedMapping));
        } catch (JsonProcessingException e) {
            datasetImport.setColumnMappingJson("{}");
        }

        return importRepository.save(datasetImport);
    }

    @Transactional(readOnly = true)
    public DatasetImport getImport(Long importId, Long workspaceId) {
        return importRepository.findByIdAndWorkspaceId(importId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Import job not found: " + importId));
    }

    @Transactional(readOnly = true)
    public List<DatasetImport> getImportHistory(Long workspaceId) {
        return importRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<DatasetImport> getRecentImports(Long workspaceId) {
        return importRepository.findTop10ByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getColumnMapping(DatasetImport datasetImport) {
        if (datasetImport.getColumnMappingJson() == null || datasetImport.getColumnMappingJson().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(datasetImport.getColumnMappingJson(), new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    @Transactional
    public void saveColumnMapping(Long importId, Long workspaceId, Map<String, String> mapping) {
        DatasetImport datasetImport = getImport(importId, workspaceId);
        try {
            datasetImport.setColumnMappingJson(objectMapper.writeValueAsString(mapping));
            importRepository.save(datasetImport);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize column mapping", e);
        }
    }

    @Transactional(readOnly = true)
    public ParsedFileData getPreviewData(DatasetImport datasetImport, int maxRows) throws IOException {
        File file = storageService.getFile(datasetImport.getStoredFilePath());
        FileParser parser = parserFactory.getParser(datasetImport.getSourceFileName());
        return parser.readPreviewAndHeaders(file, maxRows);
    }

    @Transactional(readOnly = true)
    public ParsedFileData getFullData(DatasetImport datasetImport) throws IOException {
        File file = storageService.getFile(datasetImport.getStoredFilePath());
        FileParser parser = parserFactory.getParser(datasetImport.getSourceFileName());
        return parser.parseFull(file);
    }

    @Transactional
    public ValidationResult validateImport(Long importId, Long workspaceId) throws IOException {
        DatasetImport datasetImport = getImport(importId, workspaceId);
        datasetImport.setStatus(ImportStatus.VALIDATING);
        importRepository.save(datasetImport);

        Map<String, String> mapping = getColumnMapping(datasetImport);
        ParsedFileData fullData = getFullData(datasetImport);

        DatasetValidator validator = validatorFactory.getValidator(datasetImport.getDatasetType());
        ValidationResult result = validator.validate(fullData.getRows(), mapping, datasetImport, workspaceId);

        // Delete any old errors
        errorRepository.deleteByDatasetImportId(datasetImport.getId());

        // Save errors and attach to import
        List<ImportError> errors = result.getErrors();
        for (ImportError err : errors) {
            err.setDatasetImport(datasetImport);
            errorRepository.save(err);
        }

        datasetImport.setTotalRows(result.getTotalRows());
        datasetImport.setValidRows(result.getValidRows());
        datasetImport.setInvalidRows(result.getInvalidRows());
        datasetImport.setDuplicateRows(result.getDuplicateRows());
        datasetImport.setQualityScore(result.getQualityScore());
        datasetImport.setCompletenessScore(result.getCompletenessScore());
        datasetImport.setValidityScore(result.getValidityScore());
        datasetImport.setUniquenessScore(result.getUniquenessScore());
        datasetImport.setConsistencyScore(result.getConsistencyScore());
        datasetImport.setReferentialScore(result.getReferentialScore());

        boolean hasCritical = errors.stream().anyMatch(e -> e.getSeverity() == ErrorSeverity.CRITICAL);
        if (hasCritical || (result.getTotalRows() > 0 && result.getValidRows() == 0)) {
            datasetImport.setStatus(ImportStatus.FAILED);
        } else {
            datasetImport.setStatus(ImportStatus.READY_FOR_CONFIRMATION);
        }

        return result;
    }

    @Transactional
    public void confirmImport(Long importId, Long workspaceId, boolean skipInvalidRows) throws IOException {
        DatasetImport datasetImport = getImport(importId, workspaceId);
        if (datasetImport.getStatus() != ImportStatus.READY_FOR_CONFIRMATION) {
            throw new IllegalStateException("Import job cannot be confirmed in current status: " + datasetImport.getStatus());
        }

        datasetImport.setStatus(ImportStatus.PROCESSING);
        importRepository.save(datasetImport);

        Map<String, String> mapping = getColumnMapping(datasetImport);
        ParsedFileData fullData = getFullData(datasetImport);

        // Invert mapping for canonical lookup: canonical -> header
        Map<String, String> canonicalToHeader = new HashMap<>();
        mapping.forEach((h, c) -> canonicalToHeader.put(c, h));

        // Fetch row indices that have unrecoverable errors if skipping invalid rows
        Set<Long> invalidRowNumbers = new HashSet<>();
        List<ImportError> errors = errorRepository.findByDatasetImportIdOrderByRowNumberAsc(datasetImport.getId());
        for (ImportError err : errors) {
            if (err.getSeverity() == ErrorSeverity.CRITICAL || err.getSeverity() == ErrorSeverity.ERROR) {
                invalidRowNumbers.add(err.getRowNumber());
            }
        }

        Dataset dataset = datasetImport.getDataset();
        Workspace workspace = datasetImport.getWorkspace();
        int importedCount = 0;

        switch (datasetImport.getDatasetType()) {
            case PRODUCT_CATALOG -> importedCount = importProducts(workspace, dataset, datasetImport, fullData, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
            case CUSTOMER_REGISTRY -> importedCount = importCustomers(workspace, dataset, datasetImport, fullData, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
            case SALES_TRANSACTIONS -> importedCount = importSales(workspace, dataset, datasetImport, fullData, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
            case INVENTORY_STOCK -> importedCount = importInventory(workspace, dataset, datasetImport, fullData, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
        }

        dataset.setRecordCount(dataset.getRecordCount() + importedCount);
        dataset.setLastUpdated(LocalDateTime.now());
        datasetRepository.save(dataset);

        if (!invalidRowNumbers.isEmpty()) {
            datasetImport.setStatus(ImportStatus.COMPLETED_WITH_ERRORS);
        } else {
            datasetImport.setStatus(ImportStatus.COMPLETED);
        }
        datasetImport.setCompletedAt(LocalDateTime.now());
        importRepository.save(datasetImport);
    }

    private int importProducts(Workspace workspace, Dataset dataset, DatasetImport datasetImport,
                               ParsedFileData data, Map<String, String> mapping, Set<Long> invalidRows, boolean skipInvalid) {
        List<Product> productsToSave = new ArrayList<>();
        for (int i = 0; i < data.getRows().size(); i++) {
            long rowNum = (long) i + 2;
            if (invalidRows.contains(rowNum) && skipInvalid) continue;

            Map<String, String> row = data.getRows().get(i);
            String sku = getVal(row, mapping, "product_id");
            String name = getVal(row, mapping, "product_name");
            if (sku == null || sku.isBlank() || name == null || name.isBlank()) continue;

            Product product = productRepository.findByWorkspaceIdAndProductId(workspace.getId(), sku.trim())
                    .orElse(new Product(workspace, dataset, sku.trim(), name.trim()));

            product.setProductName(name.trim());
            product.setCategory(getValOrDefault(row, mapping, "category", "General"));
            product.setSubcategory(getVal(row, mapping, "subcategory"));
            product.setBrand(getVal(row, mapping, "brand"));
            product.setSupplier(getVal(row, mapping, "supplier"));

            String costVal = getVal(row, mapping, "unit_cost");
            if (costVal != null) {
                BigDecimal cost = DataCleaningUtils.cleanDecimal(costVal);
                if (cost != null) product.setUnitCost(cost);
            }

            String priceVal = getVal(row, mapping, "selling_price");
            if (priceVal != null) {
                BigDecimal price = DataCleaningUtils.cleanDecimal(priceVal);
                if (price != null) product.setSellingPrice(price);
            }

            String expiryVal = getVal(row, mapping, "expiry_days");
            if (expiryVal != null) {
                product.setExpiryDays(DataCleaningUtils.cleanInteger(expiryVal));
            }

            product.setDatasetImport(datasetImport);
            productsToSave.add(product);
        }

        productRepository.saveAll(productsToSave);
        return productsToSave.size();
    }

    private int importCustomers(Workspace workspace, Dataset dataset, DatasetImport datasetImport,
                                ParsedFileData data, Map<String, String> mapping, Set<Long> invalidRows, boolean skipInvalid) {
        List<Customer> customersToSave = new ArrayList<>();
        for (int i = 0; i < data.getRows().size(); i++) {
            long rowNum = (long) i + 2;
            if (invalidRows.contains(rowNum) && skipInvalid) continue;

            Map<String, String> row = data.getRows().get(i);
            String cid = getVal(row, mapping, "customer_id");
            if (cid == null || cid.isBlank()) continue;

            Customer customer = customerRepository.findByWorkspaceIdAndCustomerId(workspace.getId(), cid.trim())
                    .orElse(new Customer(workspace, dataset, cid.trim()));

            customer.setCustomerName(getVal(row, mapping, "customer_name"));
            customer.setEmail(getVal(row, mapping, "email"));
            customer.setPhone(getVal(row, mapping, "phone"));
            customer.setGender(getVal(row, mapping, "gender"));
            customer.setCity(getVal(row, mapping, "city"));
            customer.setCustomerSegment(getValOrDefault(row, mapping, "customer_segment", "REGULAR"));

            String regDateVal = getVal(row, mapping, "registration_date");
            if (regDateVal != null) {
                LocalDate regDate = DataCleaningUtils.cleanDate(regDateVal);
                if (regDate != null) customer.setRegistrationDate(regDate);
            }

            String ageVal = getVal(row, mapping, "age");
            if (ageVal != null) {
                customer.setAge(DataCleaningUtils.cleanInteger(ageVal));
            }

            customer.setDatasetImport(datasetImport);
            customersToSave.add(customer);
        }

        customerRepository.saveAll(customersToSave);
        return customersToSave.size();
    }

    private int importSales(Workspace workspace, Dataset dataset, DatasetImport datasetImport,
                            ParsedFileData data, Map<String, String> mapping, Set<Long> invalidRows, boolean skipInvalid) {
        List<SalesTransaction> txnsToSave = new ArrayList<>();
        for (int i = 0; i < data.getRows().size(); i++) {
            long rowNum = (long) i + 2;
            if (invalidRows.contains(rowNum) && skipInvalid) continue;

            Map<String, String> row = data.getRows().get(i);
            String txnId = getVal(row, mapping, "transaction_id");
            String timeRaw = getVal(row, mapping, "transaction_date");
            String prodId = getVal(row, mapping, "product_id");
            String qtyRaw = getVal(row, mapping, "quantity");
            String priceRaw = getVal(row, mapping, "unit_price");

            if (txnId == null || txnId.isBlank() || timeRaw == null || prodId == null || qtyRaw == null || priceRaw == null) {
                continue;
            }

            LocalDateTime timestamp = DataCleaningUtils.cleanDateTime(timeRaw);
            Integer quantity = DataCleaningUtils.cleanInteger(qtyRaw);
            BigDecimal unitPrice = DataCleaningUtils.cleanDecimal(priceRaw);

            if (timestamp == null || quantity == null || unitPrice == null) continue;

            BigDecimal discount = BigDecimal.ZERO;
            String discRaw = getVal(row, mapping, "discount");
            if (discRaw != null && !discRaw.isBlank()) {
                BigDecimal d = DataCleaningUtils.cleanDecimal(discRaw);
                if (d != null) discount = d;
            }

            BigDecimal revenue = SalesTransaction.calculateRevenue(quantity, unitPrice, discount);
            String paymentMethod = DataCleaningUtils.normalizePaymentMethod(getVal(row, mapping, "payment_method"));

            SalesTransaction txn = new SalesTransaction(
                    workspace,
                    dataset,
                    txnId.trim(),
                    timestamp,
                    prodId.trim(),
                    quantity,
                    unitPrice,
                    revenue,
                    paymentMethod
            );

            txn.setCustomerId(getVal(row, mapping, "customer_id"));
            txn.setDiscount(discount);
            txn.setStoreId(getValOrDefault(row, mapping, "store_id", "Main Store"));
            txn.setCashierName(getVal(row, mapping, "cashier_name"));
            txn.setStatus(getValOrDefault(row, mapping, "status", "COMPLETED"));
            txn.setDatasetImport(datasetImport);

            txnsToSave.add(txn);
        }

        salesTransactionRepository.saveAll(txnsToSave);
        return txnsToSave.size();
    }

    private int importInventory(Workspace workspace, Dataset dataset, DatasetImport datasetImport,
                                ParsedFileData data, Map<String, String> mapping, Set<Long> invalidRows, boolean skipInvalid) {
        List<InventorySnapshot> snapshotsToSave = new ArrayList<>();
        for (int i = 0; i < data.getRows().size(); i++) {
            long rowNum = (long) i + 2;
            if (invalidRows.contains(rowNum) && skipInvalid) continue;

            Map<String, String> row = data.getRows().get(i);
            String dateRaw = getVal(row, mapping, "snapshot_date");
            String prodId = getVal(row, mapping, "product_id");
            String closingRaw = getVal(row, mapping, "closing_stock");

            if (dateRaw == null || prodId == null || closingRaw == null) continue;

            LocalDate snapshotDate = DataCleaningUtils.cleanDate(dateRaw);
            Integer closingStock = DataCleaningUtils.cleanInteger(closingRaw);
            if (snapshotDate == null || closingStock == null) continue;

            String invIdRaw = getVal(row, mapping, "inventory_id");
            String invId = (invIdRaw != null && !invIdRaw.isBlank())
                    ? invIdRaw.trim()
                    : "INV_" + snapshotDate + "_" + prodId.trim();

            InventorySnapshot snapshot = new InventorySnapshot(
                    workspace,
                    dataset,
                    invId,
                    snapshotDate,
                    prodId.trim(),
                    closingStock
            );

            String openingRaw = getVal(row, mapping, "opening_stock");
            if (openingRaw != null) snapshot.setOpeningStock(DataCleaningUtils.cleanInteger(openingRaw));

            String recRaw = getVal(row, mapping, "received_stock");
            if (recRaw != null) snapshot.setStockReceived(DataCleaningUtils.cleanInteger(recRaw));

            String soldRaw = getVal(row, mapping, "sold_quantity");
            if (soldRaw != null) snapshot.setStockSold(DataCleaningUtils.cleanInteger(soldRaw));

            String costRaw = getVal(row, mapping, "unit_cost");
            if (costRaw != null) snapshot.setUnitCost(DataCleaningUtils.cleanDecimal(costRaw));

            String reorderRaw = getVal(row, mapping, "reorder_level");
            if (reorderRaw != null) snapshot.setReorderLevel(DataCleaningUtils.cleanInteger(reorderRaw));

            String locRaw = getVal(row, mapping, "warehouse_or_store");
            if (locRaw != null) snapshot.setWarehouseLocation(locRaw.trim());

            snapshot.setDatasetImport(datasetImport);
            snapshotsToSave.add(snapshot);
        }

        inventorySnapshotRepository.saveAll(snapshotsToSave);
        return snapshotsToSave.size();
    }

    private String getVal(Map<String, String> row, Map<String, String> mapping, String canonical) {
        String header = mapping.get(canonical);
        String val = header != null ? row.get(header) : row.get(canonical);
        return val != null && !val.isBlank() ? val.trim() : null;
    }

    private String getValOrDefault(Map<String, String> row, Map<String, String> mapping, String canonical, String defaultVal) {
        String val = getVal(row, mapping, canonical);
        return val != null ? val : defaultVal;
    }

    @Transactional
    public void cancelImport(Long importId, Long workspaceId) {
        DatasetImport datasetImport = getImport(importId, workspaceId);
        datasetImport.setStatus(ImportStatus.CANCELLED);
        importRepository.save(datasetImport);
        if (datasetImport.getStoredFilePath() != null) {
            storageService.deleteFile(datasetImport.getStoredFilePath());
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportErrorsCsv(Long importId, Long workspaceId) {
        getImport(importId, workspaceId); // Validate ownership
        List<ImportError> errors = errorRepository.findByDatasetImportIdOrderByRowNumberAsc(importId);

        StringBuilder sb = new StringBuilder();
        sb.append("Row Number,Column Name,Invalid Value,Severity,Error Code,Error Message,Suggestion\n");

        for (ImportError err : errors) {
            sb.append(err.getRowNumber() != null ? err.getRowNumber() : "").append(",");
            sb.append(escapeCsv(err.getColumnName())).append(",");
            sb.append(escapeCsv(err.getInvalidValue())).append(",");
            sb.append(err.getSeverity() != null ? err.getSeverity().name() : "").append(",");
            sb.append(escapeCsv(err.getErrorCode())).append(",");
            sb.append(escapeCsv(err.getErrorMessage())).append(",");
            sb.append(escapeCsv(err.getSuggestion())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        String s = value.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }

    @Transactional(readOnly = true)
    public List<ImportError> getImportErrors(Long importId, Long workspaceId) {
        getImport(importId, workspaceId);
        return errorRepository.findByDatasetImportIdOrderByRowNumberAsc(importId);
    }
}
