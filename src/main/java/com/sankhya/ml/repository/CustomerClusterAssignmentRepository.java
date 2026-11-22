package com.sankhya.ml.repository;

import com.sankhya.ml.entity.CustomerClusterAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerClusterAssignmentRepository extends JpaRepository<CustomerClusterAssignment, Long> {
    List<CustomerClusterAssignment> findByExecutionId(Long executionId);
    List<CustomerClusterAssignment> findByWorkspaceIdOrderByClusterIdAsc(Long workspaceId);
    
    @Query("SELECT c.clusterId, c.clusterLabel, COUNT(c), AVG(c.recencyDays), AVG(c.frequency), AVG(c.monetary), AVG(c.averageTransactionValue) " +
           "FROM CustomerClusterAssignment c WHERE c.execution.id = :executionId GROUP BY c.clusterId, c.clusterLabel ORDER BY c.clusterId")
    List<Object[]> getClusterSummaries(@Param("executionId") Long executionId);
    
    void deleteByExecutionId(Long executionId);
}
