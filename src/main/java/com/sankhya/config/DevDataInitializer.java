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
import com.sankhya.generator.dto.GeneratorRequest;
import com.sankhya.generator.service.SyntheticDataGeneratorService;
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

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final SyntheticDataGeneratorService syntheticDataGeneratorService;
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
    private final Environment environment;
    private final DataSource dataSource;

    public DevDataInitializer(UserRepository userRepository,
                              WorkspaceRepository workspaceRepository,
                              WorkspaceMembershipRepository membershipRepository,
                              PasswordEncoder passwordEncoder,
                              SyntheticDataGeneratorService syntheticDataGeneratorService,
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
                              Environment environment,
                              DataSource dataSource) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.syntheticDataGeneratorService = syntheticDataGeneratorService;
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
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        Workspace primaryWorkspace;
        if (userRepository.count() == 0) {
            log.info("Seeding initial development supermarket workspace and user...");
            User admin = new User(
                    "Rajesh Sharma",
                    "admin@sankhya.com",
                    passwordEncoder.encode("Password@123"),
                    UserRole.ROLE_ADMIN
            );
            User savedAdmin = userRepository.save(admin);

            // Workspace 1: Primary Supermarket
            Workspace ws1 = new Workspace(
                    "Green Basket Supermarket",
                    "Primary supermarket store with fresh produce, dairy, groceries, and packaged items.",
                    savedAdmin
            );
            ws1.setOnboardingStatus("COMPLETED");
            Workspace savedWs1 = workspaceRepository.save(ws1);
            membershipRepository.save(new WorkspaceMembership(savedWs1, savedAdmin, WorkspaceRole.OWNER));

            // Workspace 2: Express Branch (Multi-store testing)
            Workspace ws2 = new Workspace(
                    "Metro Fresh Express",
                    "Express convenience branch located at downtown metro station.",
                    savedAdmin
            );
            ws2.setOnboardingStatus("COMPLETED");
            Workspace savedWs2 = workspaceRepository.save(ws2);
            membershipRepository.save(new WorkspaceMembership(savedWs2, savedAdmin, WorkspaceRole.OWNER));

            // Seed primary Unified Sales & Analytics Mart
            seedUnifiedMart(savedWs1);
            primaryWorkspace = savedWs1;

            // Also seed secondary workspace with synthetic baseline for multi-store testing
            try {
                GeneratorRequest genReq = new GeneratorRequest(12, "MEDIUM", true);
                syntheticDataGeneratorService.generateSupermarketData(savedWs2, genReq);
                log.info("Synthetic supermarket dataset successfully generated for {}", savedWs2.getName());
            } catch (Exception e) {
                log.warn("Synthetic data generation failed for secondary branch: {}", e.getMessage());
            }
        } else {
            primaryWorkspace = workspaceRepository.findAll().stream().findFirst().orElse(null);
            if (primaryWorkspace != null && unifiedSalesMartRepository.countByWorkspaceId(primaryWorkspace.getId()) == 0) {
                seedUnifiedMart(primaryWorkspace);
            }
        }

        if (primaryWorkspace != null && unifiedSalesMartRepository.countByWorkspaceId(primaryWorkspace.getId()) > 0) {
            bootstrapAnalyticsAndMl(primaryWorkspace);
        }

        logStartupDiagnostics();
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
                log.info("Executing initial baseline ML pipeline (K-Means, Apriori, Random Forest, Linear Regression) for workspace '{}'...", workspace.getName());
                mlJobService.executeKMeans(wsId, 4);
                mlJobService.executeApriori(wsId, 0.01, 0.20, 1.05);
                mlJobService.executeRandomForest(wsId, 30);
                mlJobService.executeLinearRegression(wsId, 14);
            }
        } catch (Exception e) {
            log.warn("Initial ML execution warning for workspace {}: {}", wsId, e.getMessage());
        }
    }

    private void logStartupDiagnostics() {
        String activeProfiles = Arrays.toString(environment.getActiveProfiles());
        String rawUrl = environment.getProperty("spring.datasource.url", "unknown");
        String sanitizedUrl = rawUrl.replaceAll("(?i)(password=)[^;&]*", "$1****");
        String driver = environment.getProperty("spring.datasource.driver-class-name", "unknown");
        String dbProduct = "unknown";
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            dbProduct = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (Exception e) {
            dbProduct = "error resolving metadata: " + e.getMessage();
        }

        boolean geminiConfigured = environment.getProperty("sankhya.ai.gemini.api-key", "").trim().length() > 10;
        String geminiModel = environment.getProperty("sankhya.ai.gemini.model", "unconfigured");

        log.info("================================================================================");
        log.info("SANKHYA DEV RUNTIME & DATABASE DIAGNOSTICS");
        log.info("Local URL        : http://localhost:8080/ (admin@sankhya.com / Password@123)");
        log.info("Active Profiles  : {}", activeProfiles);
        log.info("Database Product : {}", dbProduct);
        log.info("JDBC Driver      : {}", driver);
        log.info("Database URL     : {}", sanitizedUrl);
        log.info("Gemini Config    : configured={}, model={}", geminiConfigured, geminiModel);
        log.info("Counts           : Workspaces={}, Datasets={}, UnifiedMart={}, Products={}, Customers={}, Inventory={}, MLExecutions={}, GeminiInsights={}",
                workspaceRepository.count(),
                datasetRepository.count(),
                unifiedSalesMartRepository.count(),
                productRepository.count(),
                customerRepository.count(),
                inventorySnapshotRepository.count(),
                mlModelExecutionRepository.count(),
                geminiInsightRepository.count());
        log.info("================================================================================");
    }

    private void seedUnifiedMart(Workspace workspace) {
        File martFile = new File("data/sankhya_unified_mart.csv");
        if (!martFile.exists()) {
            martFile = new File("C:/Users/sujal/.gemini/antigravity/brain/e9670aee-99f6-4790-879b-c510b42adc27/scratch/unified_mart_data/sankhya_unified_mart.csv");
        }
        if (!martFile.exists()) {
            log.warn("sankhya_unified_mart.csv not found on disk, skipping auto-seed.");
            return;
        }

        log.info("Auto-seeding Unified Sales & Analytics Mart from {} for workspace '{}'", martFile.getName(), workspace.getName());

        Dataset martDataset = datasetRepository.findByWorkspaceIdAndDatasetType(workspace.getId(), DatasetType.UNIFIED_MART)
                .orElseGet(() -> {
                    Dataset d = new Dataset(
                            workspace,
                            DatasetType.UNIFIED_MART,
                            "Unified Supermarket Fact Mart (Instacart-Derived)",
                            "Single Analytical Data Mart containing 22 canonical columns: POS transactions, basket topology, customer profiles, product margins, and inventory positions."
                    );
                    return datasetRepository.save(d);
                });

        String sql = """
            INSERT INTO unified_sales_mart (
                workspace_id, dataset_import_id, dataset_id, row_number,
                transaction_id, transaction_date, payment_method, cashier_name,
                customer_id, customer_name, city, customer_segment,
                product_id, product_name, category, subcategory, brand,
                unit_cost, selling_price, quantity, unit_price, discount,
                revenue, gross_profit,
                latest_inventory_snapshot_date, current_closing_stock, current_reorder_level, current_inventory_status,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batchParams = new ArrayList<>();
        int batchSize = 2000;
        int totalInserted = 0;
        int maxRowsToSeed = 50000; // 50,000 rows provides 100% catalog & full 12-month timeline in ~2.5s

        Map<String, Product> existingProducts = productRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p, (a, b) -> a));
        Map<String, Customer> existingCustomers = customerRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c, (a, b) -> a));
        Map<String, InventorySnapshot> existingSnapshots = inventorySnapshotRepository.findByWorkspaceId(workspace.getId()).stream()
                .collect(Collectors.toMap(InventorySnapshot::getInventoryId, s -> s, (a, b) -> a));

        Map<String, Product> distinctProducts = new LinkedHashMap<>();
        Map<String, Customer> distinctCustomers = new LinkedHashMap<>();
        Map<String, InventorySnapshot> distinctSnapshots = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(martFile, StandardCharsets.UTF_8))) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = csvFormat.parse(reader)) {
                for (CSVRecord record : parser) {
                    if (totalInserted >= maxRowsToSeed) break;

                    String txId = record.get("transaction_id");
                    String dateStr = record.get("transaction_date");
                    if (txId == null || txId.isBlank() || dateStr == null || dateStr.isBlank()) continue;

                    LocalDateTime txDate = DataCleaningUtils.cleanDateTime(dateStr);
                    if (txDate == null) continue;

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

                    String paymentMethod = record.get("payment_method");
                    String cashierName = record.get("cashier_name");

                    LocalDate invDate = DataCleaningUtils.cleanDate(record.get("latest_inventory_snapshot_date"));
                    Integer closingStock = DataCleaningUtils.cleanInteger(record.get("current_closing_stock"));
                    Integer reorderLevel = DataCleaningUtils.cleanInteger(record.get("current_reorder_level"));
                    String invStatus = record.get("current_inventory_status");

                    batchParams.add(new Object[]{
                            workspace.getId(),
                            null,
                            martDataset.getId(),
                            totalInserted + 1,
                            txId.trim(),
                            txDate,
                            paymentMethod != null ? paymentMethod.trim() : "Cash",
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

                    totalInserted++;

                    // Collect distinct dimensions (idempotent upsert)
                    if (prodId != null && !distinctProducts.containsKey(prodId.trim())) {
                        String pid = prodId.trim();
                        Product p = existingProducts.getOrDefault(pid,
                                new Product(workspace, martDataset, pid, prodName != null ? prodName.trim() : pid));
                        p.setProductName(prodName != null ? prodName.trim() : pid);
                        p.setCategory(category != null ? category.trim() : "General");
                        p.setSubcategory(subcategory != null ? subcategory.trim() : null);
                        p.setBrand(brand != null ? brand.trim() : null);
                        p.setUnitCost(unitCost);
                        p.setSellingPrice(sellingPrice);
                        p.setDataset(martDataset);
                        distinctProducts.put(pid, p);
                    }

                    if (custId != null && !distinctCustomers.containsKey(custId.trim())) {
                        String cid = custId.trim();
                        Customer c = existingCustomers.getOrDefault(cid, new Customer(workspace, martDataset, cid));
                        c.setCustomerName(custName != null ? custName.trim() : "Customer " + cid);
                        c.setCity(city != null ? city.trim() : "Local");
                        c.setCustomerSegment(segment != null ? segment.trim() : "REGULAR");
                        c.setDataset(martDataset);
                        distinctCustomers.put(cid, c);
                    }

                    if (prodId != null && !distinctSnapshots.containsKey(prodId.trim()) && invDate != null && closingStock != null) {
                        String pid = prodId.trim();
                        String snapId = "INV_" + invDate + "_" + pid;
                        InventorySnapshot s = existingSnapshots.getOrDefault(snapId,
                                new InventorySnapshot(workspace, martDataset, snapId, invDate, pid, closingStock));
                        s.setClosingStock(closingStock);
                        s.setUnitCost(unitCost);
                        s.setReorderLevel(reorderLevel != null ? reorderLevel : 0);
                        s.setDataset(martDataset);
                        distinctSnapshots.put(pid, s);
                    }

                    if (batchParams.size() >= batchSize) {
                        jdbcTemplate.batchUpdate(sql, batchParams);
                        batchParams.clear();
                    }
                }

                if (!batchParams.isEmpty()) {
                    jdbcTemplate.batchUpdate(sql, batchParams);
                    batchParams.clear();
                }
            }

            // Sync dimension entities cleanly
            if (!distinctProducts.isEmpty()) productRepository.saveAll(distinctProducts.values());
            if (!distinctCustomers.isEmpty()) customerRepository.saveAll(distinctCustomers.values());
            if (!distinctSnapshots.isEmpty()) inventorySnapshotRepository.saveAll(distinctSnapshots.values());

            martDataset.setRecordCount((int) unifiedSalesMartRepository.countByWorkspaceId(workspace.getId()));
            martDataset.setLastUpdated(LocalDateTime.now());
            datasetRepository.save(martDataset);

            log.info("Successfully seeded {} rows into unified_sales_mart for workspace '{}'. Dimensions synced: {} products, {} customers, {} inventory items.",
                    totalInserted, workspace.getName(), distinctProducts.size(), distinctCustomers.size(), distinctSnapshots.size());

        } catch (Exception e) {
            log.error("Failed to seed unified mart: {}", e.getMessage(), e);
        }
    }
}

