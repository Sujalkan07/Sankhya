package com.sankhya.workspace.repository;

import com.sankhya.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findByOwnerId(Long ownerId);

    Optional<Workspace> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("SELECT DISTINCT w FROM Workspace w " +
           "LEFT JOIN w.memberships m " +
           "WHERE w.owner.id = :userId OR m.user.id = :userId " +
           "ORDER BY w.createdAt DESC")
    List<Workspace> findAllAccessibleByUserId(@Param("userId") Long userId);
}
