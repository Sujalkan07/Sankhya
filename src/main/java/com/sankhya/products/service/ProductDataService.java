package com.sankhya.products.service;

import com.sankhya.products.entity.Product;
import com.sankhya.products.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductDataService {

    private final ProductRepository productRepository;

    public ProductDataService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public long getTotalProductsCount(Long workspaceId) {
        return productRepository.countByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public Page<Product> getProducts(Long workspaceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "productName"));
        return productRepository.findByWorkspaceId(workspaceId, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long workspaceId, String productId) {
        return productRepository.findByWorkspaceIdAndProductId(workspaceId, productId);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories(Long workspaceId) {
        return productRepository.findDistinctCategoriesByWorkspaceId(workspaceId);
    }
}
