package com.serviplus.apicontabilidad.integration;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;

// TestRestTemplate was removed in Spring Boot 4.x — use RestTemplate + makeClient()
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

    @LocalServerPort
    protected int port;

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected RestTemplate makeClient() {
        RestTemplate rt = new RestTemplate();
        // Never throw on 4xx/5xx — tests assert on response status directly
        rt.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        return rt;
    }
}
