package com.sankhya.inventory.service;

import com.sankhya.inventory.entity.InventorySnapshot;
import com.sankhya.inventory.repository.InventorySnapshotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class InventoryDataService {

    private final InventorySnapshotRepository inventorySnapshotRepository;

    public InventoryDataService(InventorySnapshotRepository inventorySnapshotRepository) {
        this.inventorySnapshotRepository = inventorySnapshotRepository;
    }

    @Transactional(readOnly = true)
    public long getTotalSnapshotsCount(Long workspaceId) {
        return inventorySnapshotRepository.countByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public Page<InventorySnapshot> getSnapshots(Long workspaceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "snapshotDate"));
        return inventorySnapshotRepository.findByWorkspaceId(workspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public List<InventorySnapshot> getSnapshotsByProduct(Long workspaceId, String productId) {
        return inventorySnapshotRepository.findByWorkspaceIdAndProductIdOrderBySnapshotDateAsc(workspaceId, productId);
    }

    @Transactional(readOnly = true)
    public List<InventorySnapshot> getSnapshotsByDate(Long workspaceId, LocalDate snapshotDate) {
        return inventorySnapshotRepository.findByWorkspaceIdAndSnapshotDate(workspaceId, snapshotDate);
    }
}
