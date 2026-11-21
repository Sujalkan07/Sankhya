package com.sankhya.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {
        "com.sankhya.user.repository",
        "com.sankhya.workspace.repository"
})
public class DatabaseConfig {
    // Phase 1 JPA Repository and transaction configuration
}
