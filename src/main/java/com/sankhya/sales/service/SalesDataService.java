package com.sankhya.sales.service;

import com.sankhya.sales.entity.SalesTransaction;
import com.sankhya.sales.repository.SalesTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalesDataService {

    private final SalesTransactionRepository salesTransactionRepository;

    public SalesDataService(SalesTransactionRepository salesTransactionRepository) {
        this.salesTransactionRepository = salesTransactionRepository;
    }

    @Transactional(readOnly = true)
    public long getTotalTransactionsCount(Long workspaceId) {
        return salesTransactionRepository.countDistinctTransactionsByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public long getTotalLineItemsCount(Long workspaceId) {
        return salesTransactionRepository.countByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue(Long workspaceId) {
        BigDecimal sum = salesTransactionRepository.sumTotalRevenueByWorkspaceId(workspaceId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public Page<SalesTransaction> getTransactions(Long workspaceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        return salesTransactionRepository.findByWorkspaceId(workspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public List<SalesTransaction> getTransactionsByDateRange(Long workspaceId, LocalDateTime start, LocalDateTime end) {
        return salesTransactionRepository.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateAsc(
                workspaceId, start, end
        );
    }
}
