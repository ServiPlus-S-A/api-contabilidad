package com.serviplus.apicontabilidad.integration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;

/**
 * Singleton Testcontainers base class — MariaDB container is started once
 * per JVM and shared across all integration test classes that extend this.
 * This avoids the expensive container-per-class startup overhead.
 */
public abstract class AbstractContainerIT {

    @Container
    static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("contabilidad_test")
            .withUsername("test")
            .withPassword("testpass");

    static {
        MARIADB.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
    }
}
