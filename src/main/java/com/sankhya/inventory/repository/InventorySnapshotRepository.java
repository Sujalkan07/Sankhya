package com.sankhya.inventory.repository;

import com.sankhya.inventory.entity.InventorySnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Long> {

    List<InventorySnapshot> findByWorkspaceId(Long workspaceId);

    Page<InventorySnapshot> findByWorkspaceId(Long workspaceId, Pageable pageable);

    List<InventorySnapshot> findByWorkspaceIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long workspaceId, LocalDate startDate, LocalDate endDate);

    List<InventorySnapshot> findByWorkspaceIdAndProductIdOrderBySnapshotDateDesc(Long workspaceId, String productId);

    List<InventorySnapshot> findByWorkspaceIdAndProductIdOrderBySnapshotDateAsc(Long workspaceId, String productId);

    List<InventorySnapshot> findByWorkspaceIdAndSnapshotDate(Long workspaceId, LocalDate snapshotDate);

    long countByWorkspaceId(Long workspaceId);

    @Query("SELECT MAX(i.snapshotDate) FROM InventorySnapshot i WHERE i.workspace.id = :workspaceId")
    Optional<LocalDate> findLatestSnapshotDate(@Param("workspaceId") Long workspaceId);

    @Query("SELECT i FROM InventorySnapshot i WHERE i.workspace.id = :workspaceId " +
           "AND i.snapshotDate = (SELECT MAX(i2.snapshotDate) FROM InventorySnapshot i2 WHERE i2.workspace.id = :workspaceId AND i2.productId = i.productId)")
    List<InventorySnapshot> findLatestSnapshotsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT i.snapshotDate, SUM(i.openingStock), SUM(i.receivedStock), SUM(i.soldQuantity), SUM(i.closingStock) " +
           "FROM InventorySnapshot i WHERE i.workspace.id = :workspaceId AND i.snapshotDate BETWEEN :startDate AND :endDate " +
           "GROUP BY i.snapshotDate ORDER BY i.snapshotDate ASC")
    List<Object[]> findDailyMovementTotalsBetween(@Param("workspaceId") Long workspaceId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT i FROM InventorySnapshot i WHERE i.workspace.id = :workspaceId AND i.closingStock <= i.reorderLevel")
    List<InventorySnapshot> findLowStockAlertsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    void deleteByWorkspaceIdAndDatasetImportId(Long workspaceId, Long datasetImportId);

    @Modifying
    @Query("DELETE FROM InventorySnapshot i WHERE i.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
