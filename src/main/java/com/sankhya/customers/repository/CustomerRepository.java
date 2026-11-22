package com.sankhya.customers.repository;

import com.sankhya.customers.entity.Customer;
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
import java.util.Set;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByWorkspaceId(Long workspaceId);

    Page<Customer> findByWorkspaceId(Long workspaceId, Pageable pageable);

    Optional<Customer> findByWorkspaceIdAndCustomerId(Long workspaceId, String customerId);

    boolean existsByWorkspaceIdAndCustomerId(Long workspaceId, String customerId);

    @Query("SELECT c.customerId FROM Customer c WHERE c.workspace.id = :workspaceId")
    Set<String> findAllCustomerIdsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    long countByWorkspaceId(Long workspaceId);

    long countByWorkspaceIdAndRegistrationDateBetween(Long workspaceId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT c.customerSegment, COUNT(c) FROM Customer c WHERE c.workspace.id = :workspaceId GROUP BY c.customerSegment")
    List<Object[]> findSegmentDistributionByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("SELECT c.city, COUNT(c) FROM Customer c WHERE c.workspace.id = :workspaceId AND c.city IS NOT NULL GROUP BY c.city ORDER BY COUNT(c) DESC")
    List<Object[]> findCityDistributionByWorkspaceId(@Param("workspaceId") Long workspaceId);

    List<Customer> findByWorkspaceIdAndCustomerSegment(Long workspaceId, String segment);

    void deleteByWorkspaceIdAndDatasetImportId(Long workspaceId, Long datasetImportId);

    @Modifying
    @Query("DELETE FROM Customer c WHERE c.workspace.id = :workspaceId")
    void deleteByWorkspaceId(@Param("workspaceId") Long workspaceId);
}
