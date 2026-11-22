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
import com.sankhya.dataset.entity.UnifiedSalesMart;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.dataset.validator.UnifiedMartValidator;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.entity.SalesTransaction;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.workspace.entity.Workspace;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DatasetImportService {

    private static final Logger log = LoggerFactory.getLogger(DatasetImportService.class);

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
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;
    private final JdbcTemplate jdbcTemplate;
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
                                UnifiedSalesMartRepository unifiedSalesMartRepository,
                                JdbcTemplate jdbcTemplate,
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
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
        this.jdbcTemplate = jdbcTemplate;
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
        File diskFile = storageService.getFile(datasetImport.getStoredFilePath());
        FileParser parser = parserFactory.getParser(datasetImport.getSourceFileName());

        ValidationResult result;
        if (datasetImport.getDatasetType() == DatasetType.UNIFIED_MART) {
            DatasetValidator validator = validatorFactory.getValidator(DatasetType.UNIFIED_MART);
            if (validator instanceof UnifiedMartValidator umv) {
                result = umv.validateStreaming(diskFile, parser, mapping, datasetImport, workspaceId);
            } else {
                ParsedFileData fullData = getFullData(datasetImport);
                result = validator.validate(fullData.getRows(), mapping, datasetImport, workspaceId);
            }
        } else {
            ParsedFileData fullData = getFullData(datasetImport);
            DatasetValidator validator = validatorFactory.getValidator(datasetImport.getDatasetType());
            result = validator.validate(fullData.getRows(), mapping, datasetImport, workspaceId);
        }

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
        File diskFile = storageService.getFile(datasetImport.getStoredFilePath());
        FileParser parser = parserFactory.getParser(datasetImport.getSourceFileName());

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

        if (datasetImport.getDatasetType() == DatasetType.UNIFIED_MART) {
            importedCount = importUnifiedMart(workspace, dataset, datasetImport, diskFile, parser, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
            dataset.setRecordCount((int) unifiedSalesMartRepository.countByWorkspaceId(workspace.getId()));
            datasetImport.setValidRows(importedCount);
        } else {
            ParsedFileData fullData = getFullData(datasetImport);
            switch (datasetImport.getDatasetType()) {
                case PRODUCT_CATALOG -> importedCount = importProducts(workspace, dataset, datasetImport, fullData, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
                case CUSTOMER_REGISTRY -> importedCount = importCustomers(workspace, dataset, datasetImport, fullData, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
                case SALES_TRANSACTIONS -> importedCount = importSales(workspace, dataset, datasetImport, fullData, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
                case INVENTORY_STOCK -> importedCount = importInventory(workspace, dataset, datasetImport, fullData, canonicalToHeader, invalidRowNumbers, skipInvalidRows);
                case UNIFIED_MART -> {}
            }
            dataset.setRecordCount(dataset.getRecordCount() + importedCount);
        }

        dataset.setLastUpdated(LocalDateTime.now());
        datasetRepository.save(dataset);

        if (importedCount > 0 && workspace != null && !"COMPLETED".equals(workspace.getOnboardingStatus())) {
            workspace.setOnboardingStatus("COMPLETED");
        }

        if (!invalidRowNumbers.isEmpty() || datasetImport.getStatus() == ImportStatus.COMPLETED_WITH_ERRORS) {
            datasetImport.setStatus(ImportStatus.COMPLETED_WITH_ERRORS);
        } else {
            datasetImport.setStatus(ImportStatus.COMPLETED);
        }
        datasetImport.setCompletedAt(LocalDateTime.now());
        importRepository.save(datasetImport);
    }

    private int importUnifiedMart(Workspace workspace, Dataset dataset, DatasetImport datasetImport,
                                  File diskFile, FileParser parser, Map<String, String> mapping,
                                  Set<Long> invalidRows, boolean skipInvalid) throws IOException {
        // Clear any previous records for this import to guarantee idempotency
        unifiedSalesMartRepository.deleteByWorkspaceIdAndDatasetImportId(workspace.getId(), datasetImport.getId());

        String sql = """
            INSERT INTO unified_sales_mart (
                workspace_id, store_id, store_name, store_owner, dataset_import_id, dataset_id, row_number,
                transaction_id, transaction_date, payment_method, cashier_name,
                customer_id, customer_name, city, customer_segment,
                product_id, product_name, category, subcategory, brand,
                unit_cost, selling_price, quantity, unit_price, discount,
                revenue, gross_profit,
                latest_inventory_snapshot_date, current_closing_stock, current_reorder_level, current_inventory_status,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        LocalDateTime now = LocalDateTime.now();
        // [0]=processedRows, [1]=skippedRows, [2]=insertedMartRows, [3]=failedInsertRows
        int[] counters = new int[4];

        // Pre-load existing workspace dimensions for idempotent upsert
        Map<String, Product> existingProducts = productRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));
        Map<String, Customer> existingCustomers = customerRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c, (a, b) -> a));
        Map<String, InventorySnapshot> existingSnapshots = inventorySnapshotRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(InventorySnapshot::getInventoryId, s -> s, (a, b) -> a));

        // Track distinct products, customers, and latest inventory for dimension synchronization
        Map<String, Product> distinctProducts = new LinkedHashMap<>();
        Map<String, Customer> distinctCustomers = new LinkedHashMap<>();
        Map<String, InventorySnapshot> distinctSnapshots = new LinkedHashMap<>();

        // Process in batches of 2,000 rows
        parser.processBatches(diskFile, 2000, batch -> {
            List<Object[]> batchParams = new ArrayList<>(batch.size());

            for (Map<String, String> row : batch) {
                counters[0]++; // processedRows
                long rowNum = counters[0] + 1L; // Row 1 is header
                if (invalidRows.contains(rowNum) && skipInvalid) {
                    counters[1]++; // skippedRows
                    continue;
                }

                String txId = getVal(row, mapping, "transaction_id");
                String txDateRaw = getVal(row, mapping, "transaction_date");
                String prodId = getVal(row, mapping, "product_id");
                String prodName = getVal(row, mapping, "product_name");
                String category = getValOrDefault(row, mapping, "category", "General");
                String qtyRaw = getVal(row, mapping, "quantity");
                String unitPriceRaw = getVal(row, mapping, "unit_price");
                String unitCostRaw = getVal(row, mapping, "unit_cost");
                String sellingPriceRaw = getVal(row, mapping, "selling_price");

                if (txId == null || txId.isBlank() || txDateRaw == null || prodId == null || prodName == null) {
                    counters[1]++; // skippedRows
                    continue;
                }

                LocalDateTime txDate = DataCleaningUtils.cleanDateTime(txDateRaw);
                Integer quantity = DataCleaningUtils.cleanInteger(qtyRaw);
                BigDecimal unitPrice = DataCleaningUtils.cleanDecimal(unitPriceRaw);
                BigDecimal unitCost = DataCleaningUtils.cleanDecimal(unitCostRaw);
                BigDecimal sellingPrice = DataCleaningUtils.cleanDecimal(sellingPriceRaw);

                if (txDate == null || quantity == null || quantity <= 0 || unitPrice == null) {
                    counters[1]++; // skippedRows
                    continue;
                }
                if (unitCost == null) unitCost = BigDecimal.ZERO;
                if (sellingPrice == null) sellingPrice = unitPrice;

                BigDecimal discount = BigDecimal.ZERO;
                String discRaw = getVal(row, mapping, "discount");
                if (discRaw != null && !discRaw.isBlank()) {
                    BigDecimal d = DataCleaningUtils.cleanDecimal(discRaw);
                    if (d != null && d.compareTo(BigDecimal.ZERO) >= 0) discount = d;
                }

                BigDecimal revenue = UnifiedSalesMart.calculateRevenue(quantity, unitPrice, discount);
                BigDecimal grossProfit = UnifiedSalesMart.calculateGrossProfit(revenue, quantity, unitCost);

                String paymentMethod = DataCleaningUtils.normalizePaymentMethod(getVal(row, mapping, "payment_method"));
                String cashierName = getVal(row, mapping, "cashier_name");
                String customerId = getVal(row, mapping, "customer_id");
                String customerName = getVal(row, mapping, "customer_name");
                String city = getVal(row, mapping, "city");
                String customerSegment = getValOrDefault(row, mapping, "customer_segment", "REGULAR");
                String subcategory = getVal(row, mapping, "subcategory");
                String brand = getVal(row, mapping, "brand");

                String storeId = getValOrDefault(row, mapping, "store_id", workspace.getStoreKey());
                String storeName = getValOrDefault(row, mapping, "store_name", workspace.getName());
                String storeOwner = getValOrDefault(row, mapping, "store_owner", workspace.getOwnerName());
                String invDateRaw = getVal(row, mapping, "latest_inventory_snapshot_date");
                LocalDate invDate = invDateRaw != null ? DataCleaningUtils.cleanDate(invDateRaw) : null;
                Integer closingStock = DataCleaningUtils.cleanInteger(getVal(row, mapping, "current_closing_stock"));
                Integer reorderLevel = DataCleaningUtils.cleanInteger(getVal(row, mapping, "current_reorder_level"));
                String invStatus = getVal(row, mapping, "current_inventory_status");

                batchParams.add(new Object[]{
                        workspace.getId(),
                        storeId,
                        storeName,
                        storeOwner,
                        datasetImport.getId(),
                        dataset.getId(),
                        (int) rowNum,
                        txId.trim(),
                        txDate,
                        paymentMethod,
                        cashierName,
                        customerId != null ? customerId.trim() : null,
                        customerName != null ? customerName.trim() : null,
                        city,
                        customerSegment,
                        prodId.trim(),
                        prodName.trim(),
                        category.trim(),
                        subcategory,
                        brand,
                        unitCost,
                        sellingPrice,
                        quantity,
                        unitPrice,
                        discount,
                        revenue,
                        grossProfit,
                        invDate,
                        closingStock,
                        reorderLevel,
                        invStatus,
                        now
                });

                // Upsert distinct Product dimension
                String normalizedProdId = prodId.trim();
                if (!distinctProducts.containsKey(normalizedProdId)) {
                    Product p = existingProducts.getOrDefault(normalizedProdId,
                            new Product(workspace, dataset, normalizedProdId, prodName.trim()));
                    p.setProductName(prodName.trim());
                    p.setCategory(category.trim());
                    p.setSubcategory(subcategory);
                    p.setBrand(brand);
                    p.setUnitCost(unitCost);
                    p.setSellingPrice(sellingPrice);
                    p.setDataset(dataset);
                    p.setDatasetImport(datasetImport);
                    distinctProducts.put(normalizedProdId, p);
                }

                // Upsert distinct Customer dimension
                if (customerId != null && !customerId.isBlank()) {
                    String normalizedCustId = customerId.trim();
                    if (!distinctCustomers.containsKey(normalizedCustId)) {
                        Customer c = existingCustomers.getOrDefault(normalizedCustId,
                                new Customer(workspace, dataset, normalizedCustId));
                        c.setCustomerName(customerName != null ? customerName.trim() : normalizedCustId);
                        c.setCity(city);
                        c.setCustomerSegment(customerSegment);
                        c.setDataset(dataset);
                        c.setDatasetImport(datasetImport);
                        distinctCustomers.put(normalizedCustId, c);
                    }
                }

                // Upsert distinct latest InventorySnapshot dimension
                if (!distinctSnapshots.containsKey(normalizedProdId) && invDate != null && closingStock != null) {
                    String snapshotId = "INV_" + invDate + "_" + normalizedProdId;
                    InventorySnapshot s = existingSnapshots.getOrDefault(snapshotId,
                            new InventorySnapshot(workspace, dataset, snapshotId, invDate, normalizedProdId, closingStock));
                    s.setClosingStock(closingStock);
                    s.setUnitCost(unitCost);
                    s.setReorderLevel(reorderLevel != null ? reorderLevel : 0);
                    s.setDataset(dataset);
                    s.setDatasetImport(datasetImport);
                    distinctSnapshots.put(normalizedProdId, s);
                }
            }

            if (!batchParams.isEmpty()) {
                try {
                    jdbcTemplate.batchUpdate(sql, batchParams);
                    counters[2] += batchParams.size(); // insertedMartRows
                } catch (Exception ex) {
                    counters[3] += batchParams.size(); // failedInsertRows
                    log.error("Batch insert failed during unified mart import for workspace {}: {}", workspace.getId(), ex.getMessage(), ex);
                    throw ex;
                }
            }
        });

        // Synchronize normalized dimension tables cleanly inside transaction (no silent swallowing)
        int syncedProducts = 0;
        int syncedCustomers = 0;
        int syncedSnapshots = 0;

        if (!distinctProducts.isEmpty()) {
            syncedProducts = productRepository.saveAll(distinctProducts.values()).size();
        }
        if (!distinctCustomers.isEmpty()) {
            syncedCustomers = customerRepository.saveAll(distinctCustomers.values()).size();
        }
        if (!distinctSnapshots.isEmpty()) {
            syncedSnapshots = inventorySnapshotRepository.saveAll(distinctSnapshots.values()).size();
        }

        String summaryMsg = String.format(
                "Processed=%d, Inserted=%d, Skipped=%d, Failed=%d, Synced[Products=%d, Customers=%d, Inventory=%d]",
                counters[0], counters[2], counters[1], counters[3], syncedProducts, syncedCustomers, syncedSnapshots);
        datasetImport.setErrorSummary(summaryMsg);
        if (counters[1] > 0 && counters[2] > 0) {
            datasetImport.setStatus(ImportStatus.COMPLETED_WITH_ERRORS);
        }
        log.info("Unified Mart import summary for workspace {}: {}", workspace.getId(), summaryMsg);

        return counters[2];
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
