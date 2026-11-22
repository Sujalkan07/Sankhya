package com.sankhya.customers.service;

import com.sankhya.customers.entity.Customer;
import com.sankhya.customers.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerDataService {

    private final CustomerRepository customerRepository;

    public CustomerDataService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public long getTotalCustomersCount(Long workspaceId) {
        return customerRepository.countByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public Page<Customer> getCustomers(Long workspaceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "customerName"));
        return customerRepository.findByWorkspaceId(workspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(Long workspaceId, String customerId) {
        return customerRepository.findByWorkspaceIdAndCustomerId(workspaceId, customerId);
    }

    @Transactional(readOnly = true)
    public List<Customer> getCustomersBySegment(Long workspaceId, String segment) {
        return customerRepository.findByWorkspaceIdAndCustomerSegment(workspaceId, segment);
    }
}
