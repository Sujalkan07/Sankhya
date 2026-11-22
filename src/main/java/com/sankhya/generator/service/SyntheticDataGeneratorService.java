package com.sankhya.generator.service;

import com.sankhya.customers.entity.Customer;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.entity.Dataset;
import com.sankhya.dataset.enums.DatasetType;
import com.sankhya.dataset.repository.DatasetRepository;
import com.sankhya.dataset.service.DatasetService;
import com.sankhya.generator.dto.GeneratorRequest;
import com.sankhya.generator.dto.GeneratorSummaryResponse;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.entity.SalesTransaction;
import com.sankhya.sales.repository.SalesTransactionRepository;
import com.sankhya.workspace.entity.Workspace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class SyntheticDataGeneratorService {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetService datasetService;

    public SyntheticDataGeneratorService(ProductRepository productRepository,
                                         CustomerRepository customerRepository,
                                         SalesTransactionRepository salesTransactionRepository,
                                         InventorySnapshotRepository inventorySnapshotRepository,
                                         DatasetRepository datasetRepository,
                                         DatasetService datasetService) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
        this.datasetRepository = datasetRepository;
        this.datasetService = datasetService;
    }

    private static final String[][] BASE_PRODUCTS = {
            {"PROD_001", "Amul Taaza Toned Milk 1L", "Dairy", "Milk & Cream", "Amul", "66.00", "74.00", "Litres", "10"},
            {"PROD_002", "Amul Salted Butter 500g", "Dairy", "Butter & Cheese", "Amul", "240.00", "275.00", "Units", "180"},
            {"PROD_003", "Mother Dairy Fresh Paneer 200g", "Dairy", "Paneer & Curd", "Mother Dairy", "82.00", "95.00", "Units", "15"},
            {"PROD_004", "Amul Cheese Slices 200g (10 slices)", "Dairy", "Butter & Cheese", "Amul", "125.00", "145.00", "Units", "180"},
            {"PROD_005", "Aashirvaad Shudh Chakki Atta 5kg", "Staples & Grains", "Flours", "ITC", "215.00", "250.00", "Kilograms", "180"},
            {"PROD_006", "Tata Salt Vacuum Evaporated 1kg", "Staples & Grains", "Salt & Sugar", "Tata Consumer", "22.00", "28.00", "Kilograms", "720"},
            {"PROD_007", "Fortune Sunlite Refined Sunflower Oil 1L", "Staples & Grains", "Edible Oils", "Adani Wilmar", "132.00", "155.00", "Litres", "270"},
            {"PROD_008", "Daawat Rozana Super Basmati Rice 5kg", "Staples & Grains", "Rice & Grains", "LT Foods", "340.00", "399.00", "Kilograms", "720"},
            {"PROD_009", "Tata Sampann Unpolished Toor Dal 1kg", "Staples & Grains", "Pulses & Dals", "Tata Consumer", "145.00", "175.00", "Kilograms", "365"},
            {"PROD_010", "Saffola Gold Pro Healthy Edible Oil 1L", "Staples & Grains", "Edible Oils", "Marico", "165.00", "190.00", "Litres", "270"},
            {"PROD_011", "Maggi 2-Minute Masala Noodles 280g", "Packaged Foods", "Instant Noodles", "Nestle", "48.00", "56.00", "Units", "270"},
            {"PROD_012", "Parle-G Gold Glucose Biscuits 250g", "Packaged Foods", "Biscuits & Cookies", "Parle", "25.00", "30.00", "Units", "180"},
            {"PROD_013", "Britannia Good Day Butter Cookies 200g", "Packaged Foods", "Biscuits & Cookies", "Britannia", "38.00", "45.00", "Units", "180"},
            {"PROD_014", "Haldiram's Nagpur Aloo Bhujia 400g", "Packaged Foods", "Namkeen & Snacks", "Haldiram's", "98.00", "115.00", "Units", "180"},
            {"PROD_015", "Lay's India's Magic Masala Chips 50g", "Packaged Foods", "Chips & Crisps", "PepsiCo", "16.00", "20.00", "Units", "120"},
            {"PROD_016", "Tata Tea Gold Leaf Tea 500g", "Beverages", "Tea & Chai", "Tata Consumer", "280.00", "325.00", "Units", "365"},
            {"PROD_017", "Nescafe Classic Instant Coffee 100g", "Beverages", "Coffee", "Nestle", "260.00", "310.00", "Units", "720"},
            {"PROD_018", "Thums Up Soft Drink Bottle 750ml", "Beverages", "Cold Drinks", "Coca-Cola", "35.00", "40.00", "Litres", "180"},
            {"PROD_019", "Frooti Fresh Mango Juice 1L", "Beverages", "Juices & Drinks", "Parle Agro", "65.00", "75.00", "Litres", "180"},
            {"PROD_020", "Surf Excel Easy Wash Detergent Powder 1kg", "Household", "Laundry Care", "HUL", "130.00", "150.00", "Kilograms", "720"},
            {"PROD_021", "Vim Dishwash Gel Lemon 500ml", "Household", "Dishwashing", "HUL", "95.00", "115.00", "Units", "720"},
            {"PROD_022", "Harpic Power Plus Toilet Cleaner 1L", "Household", "Bathroom Cleaning", "Reckitt", "165.00", "195.00", "Units", "720"},
            {"PROD_023", "Dettol Original Liquid Handwash 200ml", "Personal Care", "Hand Hygiene", "Reckitt", "80.00", "99.00", "Units", "720"},
            {"PROD_024", "Colgate Strong Teeth Toothpaste 200g", "Personal Care", "Oral Care", "Colgate-Palmolive", "105.00", "125.00", "Units", "720"},
            {"PROD_025", "Dove Cream Beauty Bathing Soap 125g (Pack of 3)", "Personal Care", "Bath & Body", "HUL", "190.00", "225.00", "Units", "720"},
            {"PROD_026", "Everest Kasmiri Lal Chilli Powder 100g", "Spices & Masalas", "Spices", "Everest", "68.00", "80.00", "Units", "365"},
            {"PROD_027", "MDH Kitchen King Masala 100g", "Spices & Masalas", "Blended Masalas", "MDH", "72.00", "85.00", "Units", "365"},
            {"PROD_028", "Kissan Fresh Tomato Ketchup 1kg", "Packaged Foods", "Sauces & Condiments", "HUL", "120.00", "145.00", "Units", "365"},
            {"PROD_029", "Epigamia Natural Greek Yogurt 100g", "Dairy", "Yogurt", "Drums Food", "45.00", "55.00", "Units", "21"},
            {"PROD_030", "Kurkure Masala Munch Crispy Snack 90g", "Packaged Foods", "Chips & Crisps", "PepsiCo", "16.00", "20.00", "Units", "150"}
    };

    private static final String[] FIRST_NAMES = {
            "Rajesh", "Priya", "Amit", "Sunita", "Vikram", "Sneha", "Rohan", "Pooja",
            "Ananya", "Suresh", "Kavita", "Rahul", "Deepa", "Sanjay", "Neha", "Arjun",
            "Divya", "Manoj", "Swati", "Karthik", "Ritu", "Gaurav", "Meera", "Aditya",
            "Shweta", "Nikhil", "Aarav", "Tanvi", "Abhishek", "Roshni"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Patel", "Verma", "Rao", "Singh", "Kulkarni", "Gupta", "Iyer",
            "Desai", "Menon", "Joshi", "Nair", "Reddy", "Kumar", "Mehta", "Bose",
            "Chatterjee", "Mishra", "Chauhan", "Agarwal"
    };

    private static final String[] CITIES = {
            "Mumbai", "Bengaluru", "Delhi NCR", "Pune", "Hyderabad", "Ahmedabad", "Chennai", "Kolkata"
    };

    private static final String[] SEGMENTS = {"REGULAR", "VIP", "OCCASIONAL", "NEW"};

    @Transactional
    public GeneratorSummaryResponse generateSupermarketData(Workspace workspace, GeneratorRequest request) {
        int durationMonths = Math.max(12, Math.min(36, request.getMonths()));
        String scale = request.getScale() != null ? request.getScale().toUpperCase() : "MEDIUM";
        Random random = new Random(42);

        if (request.isClearExisting()) {
            clearWorkspaceData(workspace.getId());
        }

        Dataset productDataset = datasetService.findOrCreateDataset(workspace, DatasetType.PRODUCT_CATALOG);
        Dataset customerDataset = datasetService.findOrCreateDataset(workspace, DatasetType.CUSTOMER_REGISTRY);
        Dataset salesDataset = datasetService.findOrCreateDataset(workspace, DatasetType.SALES_TRANSACTIONS);
        Dataset inventoryDataset = datasetService.findOrCreateDataset(workspace, DatasetType.INVENTORY_STOCK);

        // 1. Generate Products
        int productLimit = switch (scale) {
            case "SMALL" -> 15;
            case "LARGE" -> BASE_PRODUCTS.length;
            default -> 25;
        };
        productLimit = Math.min(productLimit, BASE_PRODUCTS.length);

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < productLimit; i++) {
            String[] p = BASE_PRODUCTS[i];
            Product prod = productRepository.findByWorkspaceIdAndProductId(workspace.getId(), p[0])
                    .orElse(new Product(workspace, productDataset, p[0], p[1]));
            prod.setProductName(p[1]);
            prod.setCategory(p[2]);
            prod.setSubcategory(p[3]);
            prod.setBrand(p[4]);
            prod.setUnitCost(new BigDecimal(p[5]));
            prod.setSellingPrice(new BigDecimal(p[6]));
            prod.setUnitOfMeasure(p[7]);
            prod.setIsActive(true);
            prod.setShelfLifeDays(Integer.parseInt(p[8]));
            products.add(prod);
        }
        products = productRepository.saveAll(products);
        productDataset.setRecordCount((long) products.size());
        productDataset.setLastUpdated(LocalDateTime.now());
        datasetRepository.save(productDataset);

        // 2. Generate Customers
        int customerCount = switch (scale) {
            case "SMALL" -> 40;
            case "LARGE" -> 200;
            default -> 80;
        };

        List<Customer> customers = new ArrayList<>();
        for (int i = 1; i <= customerCount; i++) {
            String custId = String.format("CUST_%04d", i);
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String fullName = firstName + " " + lastName;
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@example.in";
            String phone = "+91 9" + (100000000 + random.nextInt(899999999));
            String city = CITIES[random.nextInt(CITIES.length)];
            String segment = SEGMENTS[random.nextInt(SEGMENTS.length)];
            int age = 21 + random.nextInt(52);
            String gender = random.nextBoolean() ? "Male" : "Female";
            LocalDate regDate = LocalDate.now().minusMonths(durationMonths).plusDays(random.nextInt(durationMonths * 28));

            Customer customer = customerRepository.findByWorkspaceIdAndCustomerId(workspace.getId(), custId)
                    .orElse(new Customer(workspace, customerDataset, custId));
            customer.setCustomerName(fullName);
            customer.setEmail(email);
            customer.setPhone(phone);
            customer.setCity(city);
            customer.setCustomerSegment(segment);
            customer.setAge(age);
            customer.setGender(gender);
            customer.setRegistrationDate(regDate);
            customers.add(customer);
        }
        customers = customerRepository.saveAll(customers);
        customerDataset.setRecordCount((long) customers.size());
        customerDataset.setLastUpdated(LocalDateTime.now());
        datasetRepository.save(customerDataset);

        // 3. Generate Sales Transactions with temporal patterns
        int baseTransactionsPerMonth = switch (scale) {
            case "SMALL" -> 60;
            case "LARGE" -> 350;
            default -> 150;
        };

        LocalDate startDate = LocalDate.now().minusMonths(durationMonths);
        LocalDate endDate = LocalDate.now();
        List<SalesTransaction> transactions = new ArrayList<>();
        long txnCounter = 10001;

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dow = currentDate.getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

            int month = currentDate.getMonthValue();
            double seasonalFactor = 1.0;
            if (month == 10 || month == 11) {
                seasonalFactor = 1.6;
            } else if (month == 3) {
                seasonalFactor = 1.3;
            } else if (month == 12 && currentDate.getDayOfMonth() >= 20) {
                seasonalFactor = 1.4;
            }

            double weekendFactor = isWeekend ? 1.4 : 1.0;
            int dailyTxnCount = (int) Math.round((baseTransactionsPerMonth / 30.0) * weekendFactor * seasonalFactor);
            dailyTxnCount = Math.max(1, dailyTxnCount + (random.nextInt(3) - 1));

            for (int t = 0; t < dailyTxnCount; t++) {
                String txnId = "TXN_" + txnCounter++;
                Customer customer = customers.get(random.nextInt(customers.size()));
                int hour = 9 + random.nextInt(13);
                int minute = random.nextInt(60);
                int second = random.nextInt(60);
                LocalDateTime txnTimestamp = LocalDateTime.of(currentDate, LocalTime.of(hour, minute, second));

                int payRand = random.nextInt(100);
                String paymentMethod = "UPI";
                if (payRand < 25) paymentMethod = "CASH";
                else if (payRand < 43) paymentMethod = "CARD";
                else if (payRand < 45) paymentMethod = "NET_BANKING";

                int itemsInBasket = 1 + random.nextInt(3);
                Set<Integer> selectedProductIndices = new HashSet<>();
                while (selectedProductIndices.size() < itemsInBasket && selectedProductIndices.size() < products.size()) {
                    selectedProductIndices.add(random.nextInt(products.size()));
                }

                for (int prodIdx : selectedProductIndices) {
                    Product prod = products.get(prodIdx);
                    int quantity = 1 + (random.nextInt(10) == 0 ? random.nextInt(3) : 0);
                    BigDecimal unitPrice = prod.getSellingPrice();
                    BigDecimal discount = BigDecimal.ZERO;
                    if (random.nextInt(5) == 0) {
                        discount = unitPrice.multiply(BigDecimal.valueOf(quantity))
                                .multiply(BigDecimal.valueOf(0.05 + (random.nextDouble() * 0.05)))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(discount);

                    SalesTransaction txn = new SalesTransaction(
                            workspace,
                            salesDataset,
                            txnId,
                            txnTimestamp,
                            prod.getProductId(),
                            quantity,
                            unitPrice,
                            totalAmount,
                            paymentMethod
                    );
                    txn.setCustomerId(customer.getCustomerId());
                    txn.setDiscount(discount);
                    txn.setStoreId("SmartMart Supermarket - Main Store");
                    txn.setCashierName(random.nextBoolean() ? "Rohan K." : "Sneha P.");
                    txn.setStatus("COMPLETED");

                    transactions.add(txn);
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        salesTransactionRepository.saveAll(transactions);
        salesDataset.setRecordCount((long) transactions.size());
        salesDataset.setLastUpdated(LocalDateTime.now());
        datasetRepository.save(salesDataset);

        // 4. Generate Inventory Snapshots
        List<InventorySnapshot> snapshots = new ArrayList<>();
        LocalDate invDate = startDate;
        Map<String, Integer> runningStock = new HashMap<>();

        for (Product p : products) {
            runningStock.put(p.getProductId(), 60 + random.nextInt(80));
        }

        while (!invDate.isAfter(endDate)) {
            for (Product p : products) {
                int opening = runningStock.get(p.getProductId());
                int received = (opening < 30) ? (50 + random.nextInt(50)) : (random.nextInt(20));
                int sold = Math.min(opening + received, 5 + random.nextInt(25));
                int closing = opening + received - sold;

                runningStock.put(p.getProductId(), closing);

                String invId = "INV_" + invDate + "_" + p.getProductId();
                InventorySnapshot snapshot = new InventorySnapshot(
                        workspace,
                        inventoryDataset,
                        invId,
                        invDate,
                        p.getProductId(),
                        closing
                );
                snapshot.setOpeningStock(opening);
                snapshot.setStockReceived(received);
                snapshot.setStockSold(sold);
                snapshot.setUnitCost(p.getUnitCost());
                snapshot.setReorderLevel(25);
                snapshot.setWarehouseLocation("Section-" + p.getCategory().substring(0, Math.min(3, p.getCategory().length())).toUpperCase());

                snapshots.add(snapshot);
            }
            invDate = invDate.plusWeeks(1);
        }

        inventorySnapshotRepository.saveAll(snapshots);
        inventoryDataset.setRecordCount((long) snapshots.size());
        inventoryDataset.setLastUpdated(LocalDateTime.now());
        datasetRepository.save(inventoryDataset);

        return new GeneratorSummaryResponse(
                true,
                String.format("Successfully synthesized Indian Supermarket dataset across %d months: %d products, %d customers, %d sales line-items, and %d inventory snapshots.",
                        durationMonths, products.size(), customers.size(), transactions.size(), snapshots.size()),
                products.size(),
                customers.size(),
                transactions.size(),
                snapshots.size(),
                durationMonths
        );
    }

    private void clearWorkspaceData(Long workspaceId) {
        inventorySnapshotRepository.deleteByWorkspaceId(workspaceId);
        salesTransactionRepository.deleteByWorkspaceId(workspaceId);
        customerRepository.deleteByWorkspaceId(workspaceId);
        productRepository.deleteByWorkspaceId(workspaceId);
    }
}
