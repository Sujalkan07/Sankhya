package com.sankhya.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
        "com.sankhya.user.repository",
        "com.sankhya.workspace.repository",
        "com.sankhya.dataset.repository",
        "com.sankhya.products.repository",
        "com.sankhya.customers.repository",
        "com.sankhya.sales.repository",
        "com.sankhya.inventory.repository",
        "com.sankhya.analytics.recommendations.repository",
        "com.sankhya.ml.repository"
})
public class DatabaseConfig {
    // JPA Repository and transaction configuration for all Sankhya modules
}
