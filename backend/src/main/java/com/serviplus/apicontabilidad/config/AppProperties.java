package com.serviplus.apicontabilidad.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Type-safe configuration bound from application.properties prefix "app".
 * Nested records map to property hierarchy (e.g., app.jwt.secret, app.iva.rate).
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        JwtConfig jwt,
        IvaConfig iva,
        MinioConfig minio,
        EmailConfig email,
        CorsConfig cors,
        String publicUrl
) {
    public record JwtConfig(String secret) {}

    public record IvaConfig(BigDecimal rate) {}

    public record MinioConfig(String endpoint, String accessKey, String secretKey, String bucket) {}

    public record EmailConfig(String from) {}

    public record CorsConfig(String allowedOrigins) {}
}
