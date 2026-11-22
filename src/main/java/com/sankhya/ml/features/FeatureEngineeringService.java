package com.sankhya.ml.features;

import com.sankhya.customers.entity.Customer;
import com.sankhya.customers.repository.CustomerRepository;
import com.sankhya.dataset.repository.UnifiedSalesMartRepository;
import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import com.sankhya.ml.enums.DemandClass;
import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureEngineeringService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SalesTransactionRepository salesTransactionRepository;
    private final UnifiedSalesMartRepository unifiedSalesMartRepository;
    private final InventorySnapshotRepository inventorySnapshotRepository;

    public FeatureEngineeringService(CustomerRepository customerRepository,
                                     ProductRepository productRepository,
                                     SalesTransactionRepository salesTransactionRepository,
                                     UnifiedSalesMartRepository unifiedSalesMartRepository,
                                     InventorySnapshotRepository inventorySnapshotRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.salesTransactionRepository = salesTransactionRepository;
        this.unifiedSalesMartRepository = unifiedSalesMartRepository;
        this.inventorySnapshotRepository = inventorySnapshotRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerFeatureVector> prepareCustomerFeatures(Long workspaceId) {
        boolean useMart = unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;
        LocalDateTime maxTxDate = useMart ? unifiedSalesMartRepository.findMaxTransactionDateByWorkspaceId(workspaceId) : null;
        LocalDateTime now = maxTxDate != null ? maxTxDate : LocalDateTime.now().plusDays(1);
        LocalDateTime oneYearAgo = now.minusYears(1);
        List<CustomerFeatureVector> vectors = new ArrayList<>();

        if (useMart) {
            List<Object[]> rfmRows = unifiedSalesMartRepository.findCustomerRfmMetricsBetween(workspaceId, oneYearAgo, now);
            long pseudoId = 1L;
            for (Object[] row : rfmRows) {
                if (row[0] == null) continue;
                String cId = row[0].toString();
                LocalDateTime lastTx = (LocalDateTime) row[4];
                double recencyDays = lastTx != null ? Math.max(0, Duration.between(lastTx, now).toDays()) : 180.0;
                double frequency = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
                double monetary = row[6] != null ? ((BigDecimal) row[6]).doubleValue() : 0.0;
                double atv = frequency > 0 ? monetary / frequency : 0.0;

                vectors.add(new CustomerFeatureVector(
                        pseudoId++,
                        cId,
                        recencyDays,
                        frequency,
                        monetary,
                        atv,
                        frequency * 2.5
                ));
            }
        } else {
            List<Customer> customers = customerRepository.findByWorkspaceId(workspaceId);
            List<Object[]> rfmRows = salesTransactionRepository.findCustomerRfmMetricsBetween(workspaceId, oneYearAgo, now);

            Map<String, Object[]> rfmMap = new HashMap<>();
            for (Object[] row : rfmRows) {
                if (row[0] != null) {
                    rfmMap.put(row[0].toString(), row);
                }
            }

            for (Customer c : customers) {
                String cId = c.getCustomerId();
                Object[] metrics = rfmMap.get(cId);

                double recencyDays = 180.0;
                double frequency = 0.0;
                double monetary = 0.0;
                double atv = 0.0;

                if (metrics != null) {
                    LocalDateTime lastTx = (LocalDateTime) metrics[1];
                    if (lastTx != null) {
                        recencyDays = Math.max(0, Duration.between(lastTx, now).toDays());
                    }
                    if (metrics[2] != null) {
                        frequency = ((Number) metrics[2]).doubleValue();
                    }
                    if (metrics[3] != null) {
                        monetary = ((BigDecimal) metrics[3]).doubleValue();
                    }
                    if (frequency > 0) {
                        atv = monetary / frequency;
                    }
                }

                vectors.add(new CustomerFeatureVector(
                        c.getId(),
                        c.getCustomerId(),
                        recencyDays,
                        frequency,
                        monetary,
                        atv,
                        frequency * 2.5
                ));
            }
        }

        return vectors;
    }

    @Transactional(readOnly = true)
    public List<TimeSeriesFeatureVector> prepareTimeSeriesFeatures(Long workspaceId) {
        boolean useMart = unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;
        LocalDateTime maxTxDate = useMart ? unifiedSalesMartRepository.findMaxTransactionDateByWorkspaceId(workspaceId) : null;
        LocalDateTime end = maxTxDate != null ? maxTxDate : LocalDateTime.now().plusDays(1);
        LocalDateTime start = end.minusDays(365);

        List<Object[]> dailyRows = useMart
                ? unifiedSalesMartRepository.findDailyAggregationsBetween(workspaceId, start, end)
                : salesTransactionRepository.findDailyAggregationsBetween(workspaceId, start, end);

        List<TimeSeriesFeatureVector> vectors = new ArrayList<>();

        if (dailyRows.isEmpty()) {
            return vectors;
        }

        double[] revenues = new double[dailyRows.size()];
        LocalDate[] dates = new LocalDate[dailyRows.size()];

        for (int i = 0; i < dailyRows.size(); i++) {
            Object[] row = dailyRows.get(i);
            Object dateObj = row[0];
            LocalDate d;
            if (dateObj instanceof Date sqlDate) {
                d = sqlDate.toLocalDate();
            } else if (dateObj instanceof LocalDate ld) {
                d = ld;
            } else {
                d = LocalDate.parse(dateObj.toString());
            }
            dates[i] = d;
            BigDecimal rev = (BigDecimal) row[1];
            revenues[i] = rev != null ? rev.doubleValue() : 0.0;
        }

        for (int i = 0; i < dailyRows.size(); i++) {
            LocalDate d = dates[i];
            double rev = revenues[i];
            double dayOfWeek = d.getDayOfWeek().getValue();
            double isWeekend = (dayOfWeek >= 6) ? 1.0 : 0.0;
            double lag1 = (i > 0) ? revenues[i - 1] : rev;
            double lag7 = (i >= 7) ? revenues[i - 7] : lag1;

            // Rolling 7-day average
            double sum7 = 0.0;
            int window = Math.min(7, i + 1);
            for (int k = 0; k < window; k++) {
                sum7 += revenues[i - k];
            }
            double rolling7Avg = sum7 / window;
            double trendIndex = (double) i;

            vectors.add(new TimeSeriesFeatureVector(d, rev, dayOfWeek, isWeekend, lag1, lag7, rolling7Avg, trendIndex));
        }

        return vectors;
    }

    @Transactional(readOnly = true)
    public List<ProductFeatureVector> prepareProductDemandFeatures(Long workspaceId) {
        boolean useMart = unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;
        LocalDateTime maxTxDate = useMart ? unifiedSalesMartRepository.findMaxTransactionDateByWorkspaceId(workspaceId) : null;
        LocalDateTime end = maxTxDate != null ? maxTxDate : LocalDateTime.now().plusDays(1);
        LocalDateTime start = end.minusDays(90);

        List<ProductFeatureVector> vectors = new ArrayList<>();
        double daysInPeriod = 90.0;

        if (useMart) {
            List<Object[]> perfRows = unifiedSalesMartRepository.findProductPerformanceBetween(workspaceId, start, end);
            List<Object[]> latestInv = unifiedSalesMartRepository.findLatestInventoryPerProductByWorkspaceId(workspaceId);
            Map<String, Object[]> invMap = new HashMap<>();
            for (Object[] invRow : latestInv) {
                if (invRow[0] != null) {
                    invMap.put(invRow[0].toString(), invRow);
                }
            }

            long pseudoId = 1L;
            for (Object[] perf : perfRows) {
                String pId = (String) perf[0];
                String pName = perf[1] != null ? perf[1].toString() : pId;
                String cat = perf[2] != null ? perf[2].toString() : "General";
                double totalUnits = perf[3] != null ? ((Number) perf[3]).doubleValue() : 0.0;
                double dailyVelocity = totalUnits / daysInPeriod;

                Object[] inv = invMap.get(pId);
                double unitPrice = (inv != null && inv[4] != null) ? ((BigDecimal) inv[4]).doubleValue() : 0.0;
                double currentStock = (inv != null && inv[6] != null) ? ((Number) inv[6]).doubleValue() : 0.0;

                DemandClass label;
                if (dailyVelocity >= 4.0) {
                    label = DemandClass.HIGH;
                } else if (dailyVelocity >= 1.0) {
                    label = DemandClass.MEDIUM;
                } else {
                    label = DemandClass.LOW;
                }

                vectors.add(new ProductFeatureVector(
                        pseudoId++,
                        pId,
                        pName,
                        cat,
                        totalUnits,
                        dailyVelocity,
                        unitPrice,
                        currentStock,
                        label
                ));
            }
        } else {
            List<Product> products = productRepository.findByWorkspaceId(workspaceId);
            List<Object[]> perfRows = salesTransactionRepository.findProductPerformanceBetween(workspaceId, start, end);
            List<InventorySnapshot> snapshots = inventorySnapshotRepository.findLatestSnapshotsByWorkspaceId(workspaceId);

            Map<String, Object[]> perfMap = new HashMap<>();
            for (Object[] row : perfRows) {
                if (row[0] != null) {
                    perfMap.put(row[0].toString(), row);
                }
            }

            Map<String, Integer> stockMap = new HashMap<>();
            for (InventorySnapshot snap : snapshots) {
                if (snap.getProductId() != null && snap.getClosingStock() != null) {
                    stockMap.put(snap.getProductId(), snap.getClosingStock());
                }
            }

            for (Product p : products) {
                String pId = p.getProductId();
                Object[] perf = perfMap.get(pId);

                double totalUnits = 0.0;
                if (perf != null && perf[3] != null) {
                    totalUnits = ((Number) perf[3]).doubleValue();
                }

                double dailyVelocity = totalUnits / daysInPeriod;
                double unitPrice = p.getSellingPrice() != null ? p.getSellingPrice().doubleValue() : 0.0;
                double currentStock = stockMap.getOrDefault(pId, 0);

                DemandClass label;
                if (dailyVelocity >= 4.0) {
                    label = DemandClass.HIGH;
                } else if (dailyVelocity >= 1.0) {
                    label = DemandClass.MEDIUM;
                } else {
                    label = DemandClass.LOW;
                }

                vectors.add(new ProductFeatureVector(
                        p.getId(),
                        p.getProductId(),
                        p.getProductName(),
                        p.getCategory(),
                        totalUnits,
                        dailyVelocity,
                        unitPrice,
                        currentStock,
                        label
                ));
            }
        }

        return vectors;
    }

    @Transactional(readOnly = true)
    public List<Set<String>> prepareMarketBaskets(Long workspaceId) {
        boolean useMart = unifiedSalesMartRepository.countByWorkspaceId(workspaceId) > 0;
        List<Object[]> rows = useMart
                ? unifiedSalesMartRepository.findBasketItemsByWorkspaceId(workspaceId)
                : salesTransactionRepository.findBasketItemsByWorkspaceId(workspaceId);

        Map<String, Set<String>> basketMap = new HashMap<>();

        for (Object[] row : rows) {
            if (row[0] == null) continue;
            String txId = row[0].toString();
            String item = row[2] != null ? row[2].toString().trim() : (row[1] != null ? row[1].toString() : "Unknown");
            basketMap.computeIfAbsent(txId, k -> new HashSet<>()).add(item);
        }

        // Filter out single-item or empty baskets
        return basketMap.values().stream()
                .filter(b -> b.size() >= 2)
                .collect(Collectors.toList());
    }
}
