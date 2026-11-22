package com.sankhya.config;

import com.sankhya.analytics.common.dto.DateRangeFilter;
import com.sankhya.analytics.common.enums.DatePreset;
import com.sankhya.analytics.recommendations.repository.BusinessRecommendationRepository;
import com.sankhya.analytics.recommendations.service.BusinessRecommendationService;
import com.sankhya.common.enums.UserRole;
import com.sankhya.common.enums.WorkspaceRole;
import com.sankhya.customers.entity.Customer;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.cleaner.DataCleaningUtils;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.repository.DatasetRepository;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.ml.execution.MLJobService;
import com.sankhya.ml.repository.GeminiInsightRepository;
import com.sankhya.ml.repository.MLModelExecutionRepository;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.user.entity.User;
import com.sankhya.user.repository.UserRepository;
import com.sankhya.workspace.entity.Workspace;
import com.sankhya.workspace.entity.WorkspaceMembership;
import com.sankhya.workspace.repository.WorkspaceMembershipRepository;
import com.sankhya.workspace.repository.WorkspaceRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Developer-controlled idempotent initializer for Naveen Store (STORE-001).
 * Seeds canonical 500,000-row unified sales mart from naveen_store_unified_mart.csv
 * and ensures all analytics, ML models, and store metadata originate from a single source of truth.
 */
@Component
@Profile({"dev", "default", "prod"})
public class DevDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    public static final String STORE_KEY = "STORE-001";
    public static final String STORE_NAME = "Naveen Store";
    public static final String STORE_OWNER = "Sujal Kansal";
    public static final long EXPECTED_MART_ROWS = 500_000L;

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;
    private final DatasetRepository datasetRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final BusinessRecommendationRepository businessRecommendationRepository;
    private final BusinessRecommendationService businessRecommendationService;
    private final MLJobService mlJobService;
    private final MLModelExecutionRepository mlModelExecutionRepository;
    private final GeminiInsightRepository geminiInsightRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Environment environment;
    private final DataSource dataSource;

    public DevDataInitializer(UserRepository userRepository,
                              WorkspaceRepository workspaceRepository,
                              WorkspaceMembershipRepository membershipRepository,
                              PasswordEncoder passwordEncoder,
                              UnifiedSalesMartRepository unifiedSalesMartRepository,
                              DatasetRepository datasetRepository,
                              ProductRepository productRepository,
                              CustomerRepository customerRepository,
                              InventorySnapshotRepository inventorySnapshotRepository,
                              BusinessRecommendationRepository businessRecommendationRepository,
                              BusinessRecommendationService businessRecommendationService,
                              MLJobService mlJobService,
                              MLModelExecutionRepository mlModelExecutionRepository,
                              GeminiInsightRepository geminiInsightRepository,
                              JdbcTemplate jdbcTemplate,
                              TransactionTemplate transactionTemplate,
                              Environment environment,
                              DataSource dataSource) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
        this.datasetRepository = datasetRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.businessRecommendationRepository = businessRecommendationRepository;
        this.businessRecommendationService = businessRecommendationService;
        this.mlJobService = mlJobService;
        this.mlModelExecutionRepository = mlModelExecutionRepository;
        this.geminiInsightRepository = geminiInsightRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        User primaryOwner = ensureUser("admin@sankhya.com", STORE_OWNER);
        User ownerAlias = ensureUser("sujal@naveenstore.com", STORE_OWNER);

        Workspace naveenStore = workspaceRepository.findAll().stream()
                .filter(w -> STORE_KEY.equalsIgnoreCase(w.getStoreKey()) || STORE_NAME.equalsIgnoreCase(w.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Workspace ws = new Workspace(
                            STORE_NAME,
                            "Single-store analytical data mart for Naveen Store (STORE-001 • Lucknow, UP).",
                            primaryOwner
                    );
                    ws.setStoreKey(STORE_KEY);
                    ws.setOwnerName(STORE_OWNER);
                    ws.setStoreFormat("Supermarket");
                    ws.setCity("Lucknow");
                    ws.setState("Uttar Pradesh");
                    ws.setCountry("India");
                    ws.setOnboardingStatus("COMPLETED");
                    return workspaceRepository.save(ws);
                });

        // Guarantee metadata fields & memberships are clean
        if (!STORE_KEY.equals(naveenStore.getStoreKey())
                || !STORE_OWNER.equals(naveenStore.getOwnerName())
                || !"Lucknow".equals(naveenStore.getCity())) {
            naveenStore.setStoreKey(STORE_KEY);
            naveenStore.setOwnerName(STORE_OWNER);
            naveenStore.setCity("Lucknow");
            naveenStore.setState("Uttar Pradesh");
            naveenStore.setCountry("India");
            naveenStore.setOnboardingStatus("COMPLETED");
            naveenStore = workspaceRepository.save(naveenStore);
        }
        ensureMembership(naveenStore, primaryOwner);
        ensureMembership(naveenStore, ownerAlias);

        long currentRows = unifiedSalesMartRepository.countByWorkspaceId(naveenStore.getId());
        if (currentRows != EXPECTED_MART_ROWS) {
            seedUnifiedMart(naveenStore, currentRows);
        } else {
            log.info("Naveen Store mart (STORE-001) already verified with {} rows; skipping re-seed.", currentRows);
        }

        if (unifiedSalesMartRepository.countByWorkspaceId(naveenStore.getId()) > 0) {
            bootstrapAnalyticsAndMl(naveenStore);
        }

        logStartupDiagnostics(naveenStore);
    }

    private User ensureUser(String email, String fullName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User(
                    fullName,
                    email,
                    passwordEncoder.encode("Password@123"),
                    UserRole.ROLE_ADMIN
            );
            return userRepository.save(user);
        });
    }

    private void ensureMembership(Workspace workspace, User user) {
        boolean isOwner = workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId());
        if (!isOwner && !membershipRepository.existsByWorkspaceIdAndUserId(workspace.getId(), user.getId())) {
            membershipRepository.save(new WorkspaceMembership(workspace, user, WorkspaceRole.OWNER));
        }
    }

    private void seedUnifiedMart(Workspace workspace, long existingRows) {
        File martFile = resolveMartFile();
        if (martFile == null) {
            log.warn("naveen_store_unified_mart.csv not found on disk; skipping seed.");
            return;
        }

        String sha256 = computeSha256(martFile);
        log.info("Initializing Naveen Store mart from {} ({} bytes, SHA-256: {})...",
                martFile.getAbsolutePath(), martFile.length(), sha256);

        if (existingRows > 0) {
            log.warn("Detected partial prior seed ({} rows) for workspace {}; clearing partial rows before clean reload.",
                    existingRows, workspace.getId());
            jdbcTemplate.update("DELETE FROM unified_sales_mart WHERE workspace_id = ?", workspace.getId());
        }

        Dataset martDataset = datasetRepository.findByWorkspaceIdAndDatasetType(workspace.getId(), DatasetType.UNIFIED_MART)
                .orElseGet(() -> datasetRepository.save(new Dataset(
                        workspace,
                        DatasetType.UNIFIED_MART,
                        "Naveen Store Lucknow Unified Analytical Mart (STORE-001)",
                        "Canonical 25-column mart for Naveen Store, Lucknow, Uttar Pradesh (Owner: Sujal Kansal • 500,000 lines, 83,439 receipts, 9,978 Lucknow customers, 1,000 SKUs, INR)."
                )));

        String insertSql = """
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
        int batchSize = 5000;
        List<Object[]> batchParams = new ArrayList<>(batchSize);
        int totalInserted = 0;

        Map<String, Product> existingProducts = productRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));
        Map<String, Customer> existingCustomers = customerRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c, (a, b) -> a));
        Map<String, InventorySnapshot> existingSnapshots = inventorySnapshotRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(InventorySnapshot::getInventoryId, s -> s, (a, b) -> a));

        Map<String, Product> distinctProducts = new LinkedHashMap<>();
        Map<String, Customer> distinctCustomers = new LinkedHashMap<>();
        Map<String, InventorySnapshot> distinctSnapshots = new LinkedHashMap<>();

        long startTime = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(martFile), StandardCharsets.UTF_8), 65536);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            for (CSVRecord record : csvParser) {
                String txId = record.get("transaction_id");
                LocalDateTime txDate = DataCleaningUtils.cleanDateTime(record.get("transaction_date"));
                if (txId == null || txId.isBlank() || txDate == null) continue;

                String storeId = record.isMapped("store_id") && !record.get("store_id").isBlank()
                        ? record.get("store_id").trim() : STORE_KEY;
                String storeName = record.isMapped("store_name") && !record.get("store_name").isBlank()
                        ? record.get("store_name").trim() : STORE_NAME;
                String storeOwner = record.isMapped("store_owner") && !record.get("store_owner").isBlank()
                        ? record.get("store_owner").trim() : STORE_OWNER;

                String custId = record.get("customer_id");
                String custName = record.get("customer_name");
                String city = record.get("city");
                String segment = record.get("customer_segment");

                String prodId = record.get("product_id");
                String prodName = record.get("product_name");
                String category = record.get("category");
                String subcategory = record.get("subcategory");
                String brand = record.get("brand");

                BigDecimal unitCost = DataCleaningUtils.cleanDecimal(record.get("unit_cost"));
                if (unitCost == null) unitCost = BigDecimal.ZERO;
                BigDecimal sellingPrice = DataCleaningUtils.cleanDecimal(record.get("selling_price"));
                if (sellingPrice == null) sellingPrice = BigDecimal.ZERO;

                Integer quantity = DataCleaningUtils.cleanInteger(record.get("quantity"));
                if (quantity == null || quantity <= 0) quantity = 1;

                BigDecimal unitPrice = DataCleaningUtils.cleanDecimal(record.get("unit_price"));
                if (unitPrice == null) unitPrice = sellingPrice;

                BigDecimal discount = DataCleaningUtils.cleanDecimal(record.get("discount"));
                if (discount == null) discount = BigDecimal.ZERO;

                BigDecimal revenue = unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(discount).max(BigDecimal.ZERO);
                BigDecimal grossProfit = revenue.subtract(unitCost.multiply(BigDecimal.valueOf(quantity)));

                String paymentMethod = DataCleaningUtils.normalizePaymentMethod(record.get("payment_method"));
                String cashierName = record.get("cashier_name");

                LocalDate invDate = DataCleaningUtils.cleanDate(record.get("latest_inventory_snapshot_date"));
                Integer closingStock = DataCleaningUtils.cleanInteger(record.get("current_closing_stock"));
                Integer reorderLevel = DataCleaningUtils.cleanInteger(record.get("current_reorder_level"));
                String invStatus = record.get("current_inventory_status");

                totalInserted++;

                batchParams.add(new Object[]{
                        workspace.getId(),
                        storeId,
                        storeName,
                        storeOwner,
                        null,
                        martDataset.getId(),
                        totalInserted,
                        txId.trim(),
                        txDate,
                        paymentMethod,
                        cashierName != null ? cashierName.trim() : "Store Staff",
                        custId != null ? custId.trim() : null,
                        custName != null ? custName.trim() : null,
                        city != null ? city.trim() : null,
                        segment != null ? segment.trim() : "REGULAR",
                        prodId != null ? prodId.trim() : null,
                        prodName != null ? prodName.trim() : null,
                        category != null ? category.trim() : "General",
                        subcategory != null ? subcategory.trim() : null,
                        brand != null ? brand.trim() : null,
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
                        invStatus != null ? invStatus.trim() : "HEALTHY",
                        now
                });

                if (prodId != null && !distinctProducts.containsKey(prodId)) {
                    Product p = existingProducts.getOrDefault(prodId,
                            new Product(workspace, martDataset, prodId, prodName != null ? prodName : prodId));
                    p.setProductName(prodName != null ? prodName : prodId);
                    p.setCategory(category != null ? category : "General");
                    p.setSubcategory(subcategory);
                    p.setBrand(brand);
                    p.setUnitCost(unitCost);
                    p.setSellingPrice(sellingPrice);
                    p.setDataset(martDataset);
                    distinctProducts.put(prodId, p);
                }

                if (custId != null && !distinctCustomers.containsKey(custId)) {
                    Customer c = existingCustomers.getOrDefault(custId, new Customer(workspace, martDataset, custId));
                    c.setCustomerName(custName != null ? custName : "Customer " + custId);
                    c.setCity(city != null ? city : "Local");
                    c.setCustomerSegment(segment != null ? segment : "REGULAR");
                    c.setDataset(martDataset);
                    distinctCustomers.put(custId, c);
                }

                if (prodId != null && !distinctSnapshots.containsKey(prodId) && invDate != null && closingStock != null) {
                    String snapId = "INV_" + invDate + "_" + prodId;
                    InventorySnapshot s = existingSnapshots.getOrDefault(snapId,
                            new InventorySnapshot(workspace, martDataset, snapId, invDate, prodId, closingStock));
                    s.setClosingStock(closingStock);
                    s.setUnitCost(unitCost);
                    s.setReorderLevel(reorderLevel != null ? reorderLevel : 0);
                    s.setDataset(martDataset);
                    distinctSnapshots.put(prodId, s);
                }

                if (batchParams.size() >= batchSize) {
                    flushBatch(insertSql, batchParams);
                    if (totalInserted % 100000 == 0) {
                        log.info("Naveen Store seed progress: {} rows inserted ({} ms)",
                                totalInserted, System.currentTimeMillis() - startTime);
                    }
                }
            }

            if (!batchParams.isEmpty()) {
                flushBatch(insertSql, batchParams);
            }

            // Persist deduplicated dimension catalogs in bounded transactions
            saveDimensionBatches(distinctProducts.values(), distinctCustomers.values(), distinctSnapshots.values());

            long verifiedCount = unifiedSalesMartRepository.countByWorkspaceId(workspace.getId());
            martDataset.setRecordCount((int) verifiedCount);
            martDataset.setDescription("SHA-256: " + sha256 + " | Verified rows: " + verifiedCount);
            martDataset.setLastUpdated(LocalDateTime.now());
            datasetRepository.save(martDataset);

            log.info("Completed Naveen Store mart seed in {} ms: {} rows inserted. Synchronized {} products, {} customers, {} inventory snapshots.",
                    System.currentTimeMillis() - startTime, verifiedCount,
                    distinctProducts.size(), distinctCustomers.size(), distinctSnapshots.size());

        } catch (Exception e) {
            log.error("Failed to seed Naveen Store unified mart: {}", e.getMessage(), e);
        }
    }

    private void flushBatch(String insertSql, List<Object[]> batchParams) {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(insertSql, batchParams));
        batchParams.clear();
    }

    private void saveDimensionBatches(Collection<Product> products,
                                      Collection<Customer> customers,
                                      Collection<InventorySnapshot> snapshots) {
        if (!products.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> productRepository.saveAll(products));
        }
        if (!customers.isEmpty()) {
            List<Customer> custList = new ArrayList<>(customers);
            int chunk = 2000;
            for (int i = 0; i < custList.size(); i += chunk) {
                List<Customer> slice = custList.subList(i, Math.min(i + chunk, custList.size()));
                transactionTemplate.executeWithoutResult(status -> customerRepository.saveAll(slice));
            }
        }
        if (!snapshots.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> inventorySnapshotRepository.saveAll(snapshots));
        }
    }

    private void bootstrapAnalyticsAndMl(Workspace workspace) {
        Long wsId = workspace.getId();
        try {
            if (businessRecommendationRepository.findActiveByWorkspaceId(wsId).isEmpty()) {
                businessRecommendationService.generateRecommendations(wsId, DateRangeFilter.of(DatePreset.LAST_30_DAYS, null, null));
            }
        } catch (Exception e) {
            log.warn("Recommendation bootstrap warning for workspace {}: {}", wsId, e.getMessage());
        }

        try {
            if (mlModelExecutionRepository.count() == 0) {
                log.info("Executing ML pipelines (K-Means, Apriori, Random Forest, Linear Regression) on Naveen Store mart...");
                mlJobService.executeKMeans(wsId, 4);
                mlJobService.executeApriori(wsId, 0.005, 0.15, 1.05);
                mlJobService.executeRandomForest(wsId, 30);
                mlJobService.executeLinearRegression(wsId, 14);
            }
        } catch (Exception e) {
            log.warn("Initial ML execution warning for workspace {}: {}", wsId, e.getMessage());
        }
    }

    private void logStartupDiagnostics(Workspace naveenStore) {
        String activeProfiles = Arrays.toString(environment.getActiveProfiles());
        String rawUrl = environment.getProperty("spring.datasource.url", "unknown");
        String sanitizedUrl = rawUrl.replaceAll("(?i)(password=)[^;&]*", "$1****");
        String dbProduct = "unknown";
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            dbProduct = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (Exception e) {
            dbProduct = "error: " + e.getMessage();
        }

        Long wsId = naveenStore.getId();
        long martRows = unifiedSalesMartRepository.countByWorkspaceId(wsId);
        long txCount = unifiedSalesMartRepository.countDistinctTransactionsByWorkspaceId(wsId);
        long custCount = unifiedSalesMartRepository.countTotalCustomersByWorkspaceId(wsId);
        long prodCount = unifiedSalesMartRepository.countTotalProductsByWorkspaceId(wsId);
        long catCount = unifiedSalesMartRepository.countDistinctCategoriesByWorkspaceId(wsId);
        LocalDateTime minDate = unifiedSalesMartRepository.findMinTransactionDateByWorkspaceId(wsId);
        LocalDateTime maxDate = unifiedSalesMartRepository.findMaxTransactionDateByWorkspaceId(wsId);

        boolean geminiConfigured = environment.getProperty("sankhya.ai.gemini.api-key", "").trim().length() > 10;
        String geminiModel = environment.getProperty("sankhya.ai.gemini.model", "unconfigured");

        log.info("================================================================================");
        log.info("SANKHYA — NAVEEN STORE (STORE-001) RUNTIME & RECONCILIATION DIAGNOSTICS");
        log.info("Local URL        : http://localhost:8080/ (admin@sankhya.com / Password@123)");
        log.info("Store & Owner    : {} ({}) | Owner: {}", naveenStore.getName(), naveenStore.getStoreKey(), naveenStore.getOwnerName());
        log.info("Active Profiles  : {} | DB: {} | URL: {}", activeProfiles, dbProduct, sanitizedUrl);
        log.info("Gemini Config    : configured={}, model={}", geminiConfigured, geminiModel);
        log.info("Mart Identity    : Rows={}, DistinctReceipts={}, Customers={}, Products={}, Categories={}",
                martRows, txCount, custCount, prodCount, catCount);
        log.info("Date Window      : {} to {}", minDate, maxDate);
        log.info("ML & AI Counts   : MLExecutions={}, GeminiInsights={}",
                mlModelExecutionRepository.count(), geminiInsightRepository.count());
        log.info("================================================================================");
    }

    private File resolveMartFile() {
        File f1 = new File("data/naveen_store_unified_mart.csv");
        if (f1.exists()) return f1;
        File f2 = new File("naveen_store_unified_mart.csv");
        if (f2.exists()) return f2;
        return null;
    }

    private String computeSha256(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[65536];
            int n;
            while ((n = fis.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unavailable";
        }
    }
}
