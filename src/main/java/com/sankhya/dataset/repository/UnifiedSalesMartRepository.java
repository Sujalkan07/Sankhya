package com.sankhya.dataset.repository;

import com.sankhya.dataset.entity.UnifiedSalesMart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UnifiedSalesMartRepository extends JpaRepository<UnifiedSalesMart, Long> {

    List<UnifiedSalesMart> findByWorkspaceId(Long workspaceId);

    Page<UnifiedSalesMart> findByWorkspaceId(Long workspaceId, Pageable pageable);

    long countByWorkspaceId(Long workspaceId);

    void deleteByWorkspaceIdAndDatasetImportId(Long workspaceId, Long datasetImportId);

    @Modifying
    @Query("DELETE FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // ================= Executive Dashboard & Core Sales KPIs =================

    @Query("SELECT COUNT(DISTINCT m.transactionId) FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId")
    long countDistinctTransactionsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT COALESCE(SUM(m.revenue), 0) FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId")
    BigDecimal sumTotalRevenueByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT COALESCE(SUM(m.revenue), 0) FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByWorkspaceIdAndDateBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(DISTINCT m.transactionId) FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate")
    long countDistinctTransactionsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate")
    long sumQuantityBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(m.discount), 0) FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumDiscountBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(m.quantity * m.unitCost), 0) FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumEstimatedCogsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(m.grossProfit), 0) FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumGrossProfitBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(DISTINCT m.customerId) FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.customerId IS NOT NULL AND TRIM(m.customerId) <> '' " +
            "AND m.transactionDate BETWEEN :startDate AND :endDate")
    long countActiveCustomersBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Daily aggregations: [date, sum(revenue), count(distinct txId), sum(qty)]
    @Query("SELECT FUNCTION('DATE', m.transactionDate), SUM(m.revenue), COUNT(DISTINCT m.transactionId), SUM(m.quantity) " +
            "FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', m.transactionDate) ORDER BY FUNCTION('DATE', m.transactionDate) ASC")
    List<Object[]> findDailyAggregationsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Payment method distribution: [paymentMethod, count(distinct txId), sum(revenue)]
    @Query("SELECT m.paymentMethod, COUNT(DISTINCT m.transactionId), SUM(m.revenue) " +
            "FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY m.paymentMethod")
    List<Object[]> findPaymentMethodDistributionBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Category sales contribution: [category, sum(revenue), sum(qty), count(distinct txId), sum(cogs)]
    @Query("SELECT m.category, SUM(m.revenue), SUM(m.quantity), COUNT(DISTINCT m.transactionId), SUM(m.quantity * m.unitCost) " +
            "FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY m.category ORDER BY SUM(m.revenue) DESC")
    List<Object[]> findCategorySalesContributionBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Product performance ranking: [productId, productName, category, sum(qty), sum(revenue), sum(cogs)]
    @Query("SELECT m.productId, m.productName, m.category, SUM(m.quantity), SUM(m.revenue), SUM(m.quantity * m.unitCost) " +
            "FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY m.productId, m.productName, m.category ORDER BY SUM(m.revenue) DESC")
    List<Object[]> findProductPerformanceBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Hourly sales velocity: [hour, count(distinct txId), sum(revenue)]
    @Query("SELECT FUNCTION('HOUR', m.transactionDate), COUNT(DISTINCT m.transactionId), SUM(m.revenue) " +
            "FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId AND m.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('HOUR', m.transactionDate) ORDER BY FUNCTION('HOUR', m.transactionDate) ASC")
    List<Object[]> findHourlySalesDistributionBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ================= Customer Intelligence =================

    @Query("SELECT COUNT(DISTINCT m.customerId) FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.customerId IS NOT NULL AND TRIM(m.customerId) <> ''")
    long countTotalCustomersByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // Customer RFM metrics: [customerId, customerName, city, segment, max(txDate), count(distinct txId), sum(revenue)]
    @Query("SELECT m.customerId, MAX(m.customerName), MAX(m.city), MAX(m.customerSegment), " +
            "MAX(m.transactionDate), COUNT(DISTINCT m.transactionId), SUM(m.revenue) " +
            "FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.customerId IS NOT NULL AND TRIM(m.customerId) <> '' " +
            "AND m.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY m.customerId")
    List<Object[]> findCustomerRfmMetricsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Distinct customer IDs with >= 2 distinct transactions in period (Returning Customers)
    @Query("SELECT m.customerId FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.customerId IS NOT NULL AND TRIM(m.customerId) <> '' " +
            "AND m.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY m.customerId HAVING COUNT(DISTINCT m.transactionId) >= 2")
    List<String> findReturningCustomerIdsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Customer segment distribution: [segment, count(distinct customerId)]
    @Query("SELECT COALESCE(m.customerSegment, 'REGULAR'), COUNT(DISTINCT m.customerId) " +
            "FROM UnifiedSalesMart m " +
            "WHERE m.workspace.id = :workspaceId AND m.customerId IS NOT NULL AND TRIM(m.customerId) <> '' " +
            "GROUP BY COALESCE(m.customerSegment, 'REGULAR')")
    List<Object[]> findCustomerSegmentDistributionByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // ================= Product & Catalog Intelligence =================

    @Query("SELECT COUNT(DISTINCT m.productId) FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId")
    long countTotalProductsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // Distinct products: [productId, productName, category, subcategory, brand, unitCost, sellingPrice]
    @Query("SELECT m.productId, MAX(m.productName), MAX(m.category), MAX(m.subcategory), MAX(m.brand), " +
            "MAX(m.unitCost), MAX(m.sellingPrice) " +
            "FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId GROUP BY m.productId")
    List<Object[]> findDistinctProductsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // ================= Latest Inventory Context (Deduplicated by Product) =================

    // Deduplicated inventory snapshot: [productId, productName, category, unitCost, sellingPrice, latestDate, closingStock, reorderLevel, status]
    @Query("SELECT m.productId, MAX(m.productName), MAX(m.category), MAX(m.unitCost), MAX(m.sellingPrice), " +
            "MAX(m.latestInventorySnapshotDate), MAX(m.currentClosingStock), MAX(m.currentReorderLevel), MAX(m.currentInventoryStatus) " +
            "FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId GROUP BY m.productId")
    List<Object[]> findLatestInventoryPerProductByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // ================= Market Basket Analysis (Apriori) =================

    // Basket items: [transactionId, productId, productName]
    @Query("SELECT m.transactionId, m.productId, m.productName " +
            "FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId ORDER BY m.transactionId")
    List<Object[]> findBasketItemsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // Paginated mart rows for data exploration
    @Query("SELECT m FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId " +
            "AND m.transactionDate BETWEEN :startDate AND :endDate ORDER BY m.transactionDate DESC")
    Page<UnifiedSalesMart> findTransactionsInDateRange(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    List<UnifiedSalesMart> findByWorkspaceIdAndTransactionId(Long workspaceId, String transactionId);

    @Query("SELECT MIN(m.transactionDate) FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId")
    LocalDateTime findMinTransactionDateByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT MAX(m.transactionDate) FROM UnifiedSalesMart m WHERE m.workspace.id = :workspaceId")
    LocalDateTime findMaxTransactionDateByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
