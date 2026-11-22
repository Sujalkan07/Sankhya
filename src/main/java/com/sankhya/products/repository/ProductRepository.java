package com.sankhya.products.repository;

import com.sankhya.products.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByWorkspaceId(Long workspaceId);

    Page<Product> findByWorkspaceId(Long workspaceId, Pageable pageable);

    Optional<Product> findByWorkspaceIdAndProductId(Long workspaceId, String productId);

    boolean existsByWorkspaceIdAndProductId(Long workspaceId, String productId);

    @Query("SELECT p.productId FROM Product p WHERE p.workspace.id = :workspaceId")
    Set<String> findAllProductIdsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    long countByWorkspaceIdAndIsActiveTrue(Long workspaceId);

    @Query("SELECT p.category, COUNT(p), AVG(p.sellingPrice), AVG(p.unitCost) FROM Product p WHERE p.workspace.id = :workspaceId GROUP BY p.category ORDER BY COUNT(p) DESC")
    List<Object[]> findCategoryProductSummaryByWorkspaceId(@Param("workspaceId") Long workspaceId);

    List<Product> findByWorkspaceIdAndCategory(Long workspaceId, String category);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.workspace.id = :workspaceId ORDER BY p.category")
    List<String> findDistinctCategoriesByWorkspaceId(@Param("workspaceId") Long workspaceId);

    void deleteByWorkspaceIdAndDatasetImportId(Long workspaceId, Long datasetImportId);

    @Modifying
    @Query("DELETE FROM Product p WHERE p.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
