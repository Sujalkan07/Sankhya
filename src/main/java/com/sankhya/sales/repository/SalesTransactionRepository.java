package com.sankhya.sales.repository;

import com.sankhya.sales.entity.SalesTransaction;
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
public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {

    List<SalesTransaction> findByWorkspaceId(Long workspaceId);

    Page<SalesTransaction> findByWorkspaceId(Long workspaceId, Pageable pageable);

    List<SalesTransaction> findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateAsc(
            Long workspaceId, LocalDateTime startDate, LocalDateTime endDate);

    List<SalesTransaction> findByWorkspaceIdAndProductId(Long workspaceId, String productId);

    List<SalesTransaction> findByWorkspaceIdAndCustomerId(Long workspaceId, String customerId);

    List<SalesTransaction> findByWorkspaceIdAndTransactionId(Long workspaceId, String transactionId);

    long countByWorkspaceId(Long workspaceId);

    void deleteByWorkspaceIdAndDatasetImportId(Long workspaceId, Long datasetImportId);

    @Modifying
    @Query("DELETE FROM SalesTransaction s WHERE s.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // ================= Phase 3 Analytical Aggregations =================

    @Query("SELECT COUNT(DISTINCT s.transactionId) FROM SalesTransaction s WHERE s.workspace.id = :workspaceId")
    long countDistinctTransactionsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT COALESCE(SUM(s.revenue), 0) FROM SalesTransaction s WHERE s.workspace.id = :workspaceId")
    BigDecimal sumTotalRevenueByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT COALESCE(SUM(s.revenue), 0) FROM SalesTransaction s WHERE s.workspace.id = :workspaceId AND s.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByWorkspaceIdAndDateBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(DISTINCT s.transactionId) FROM SalesTransaction s WHERE s.workspace.id = :workspaceId AND s.transactionDate BETWEEN :startDate AND :endDate")
    long countDistinctTransactionsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM SalesTransaction s WHERE s.workspace.id = :workspaceId AND s.transactionDate BETWEEN :startDate AND :endDate")
    long sumQuantityBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(s.discount), 0) FROM SalesTransaction s WHERE s.workspace.id = :workspaceId AND s.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumDiscountBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Estimated COGS: sum of (quantity * product.unitCost)
    @Query("SELECT COALESCE(SUM(s.quantity * p.unitCost), 0) FROM SalesTransaction s, Product p " +
            "WHERE s.productId = p.productId AND s.workspace.id = :workspaceId AND p.workspace.id = :workspaceId " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumEstimatedCogsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Active registered customers in period (excluding null or blank customer IDs)
    @Query("SELECT COUNT(DISTINCT s.customerId) FROM SalesTransaction s " +
            "WHERE s.workspace.id = :workspaceId AND s.customerId IS NOT NULL AND TRIM(s.customerId) <> '' " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate")
    long countActiveCustomersBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Daily revenue aggregation: [date, sum(revenue), count(distinct txId), sum(qty)]
    @Query("SELECT FUNCTION('DATE', s.transactionDate), SUM(s.revenue), COUNT(DISTINCT s.transactionId), SUM(s.quantity) " +
            "FROM SalesTransaction s WHERE s.workspace.id = :workspaceId AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', s.transactionDate) ORDER BY FUNCTION('DATE', s.transactionDate) ASC")
    List<Object[]> findDailyAggregationsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Payment method distribution: [paymentMethod, count(distinct txId), sum(revenue)]
    @Query("SELECT s.paymentMethod, COUNT(DISTINCT s.transactionId), SUM(s.revenue) " +
            "FROM SalesTransaction s WHERE s.workspace.id = :workspaceId AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY s.paymentMethod")
    List<Object[]> findPaymentMethodDistributionBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Category sales contribution: [category, sum(revenue), sum(qty), count(distinct txId)]
    @Query("SELECT p.category, SUM(s.revenue), SUM(s.quantity), COUNT(DISTINCT s.transactionId), SUM(s.quantity * p.unitCost) " +
            "FROM SalesTransaction s, Product p " +
            "WHERE s.productId = p.productId AND s.workspace.id = :workspaceId AND p.workspace.id = :workspaceId " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY p.category ORDER BY SUM(s.revenue) DESC")
    List<Object[]> findCategorySalesContributionBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Product performance ranking: [productId, productName, category, sum(qty), sum(revenue), sum(cogs)]
    @Query("SELECT s.productId, p.productName, p.category, SUM(s.quantity), SUM(s.revenue), SUM(s.quantity * p.unitCost) " +
            "FROM SalesTransaction s, Product p " +
            "WHERE s.productId = p.productId AND s.workspace.id = :workspaceId AND p.workspace.id = :workspaceId " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY s.productId, p.productName, p.category ORDER BY SUM(s.revenue) DESC")
    List<Object[]> findProductPerformanceBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Hourly sales velocity: [hour, count(distinct txId), sum(revenue)]
    @Query("SELECT FUNCTION('HOUR', s.transactionDate), COUNT(DISTINCT s.transactionId), SUM(s.revenue) " +
            "FROM SalesTransaction s WHERE s.workspace.id = :workspaceId AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('HOUR', s.transactionDate) ORDER BY FUNCTION('HOUR', s.transactionDate) ASC")
    List<Object[]> findHourlySalesDistributionBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Customer RFM metrics: [customerId, max(txDate), count(distinct txId), sum(revenue)]
    @Query("SELECT s.customerId, MAX(s.transactionDate), COUNT(DISTINCT s.transactionId), SUM(s.revenue) " +
            "FROM SalesTransaction s " +
            "WHERE s.workspace.id = :workspaceId AND s.customerId IS NOT NULL AND TRIM(s.customerId) <> '' " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY s.customerId")
    List<Object[]> findCustomerRfmMetricsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // List of distinct customer IDs with >= 2 distinct transactions in period (Returning Customers)
    @Query("SELECT s.customerId FROM SalesTransaction s " +
            "WHERE s.workspace.id = :workspaceId AND s.customerId IS NOT NULL AND TRIM(s.customerId) <> '' " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY s.customerId HAVING COUNT(DISTINCT s.transactionId) >= 2")
    List<String> findReturningCustomerIdsBetween(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Market Basket extraction for Apriori: [transactionId, productId, productName]
    @Query("SELECT s.transactionId, s.productId, COALESCE(p.productName, s.productId) " +
            "FROM SalesTransaction s LEFT JOIN Product p ON s.productId = p.productId AND p.workspace.id = :workspaceId " +
            "WHERE s.workspace.id = :workspaceId ORDER BY s.transactionId")
    List<Object[]> findBasketItemsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // Detailed transactions query with pagination
    @Query("SELECT s FROM SalesTransaction s WHERE s.workspace.id = :workspaceId " +
            "AND s.transactionDate BETWEEN :startDate AND :endDate ORDER BY s.transactionDate DESC")
    Page<SalesTransaction> findTransactionsInDateRange(
            @Param("workspaceId") Long workspaceId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
