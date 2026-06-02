package com.serviplus.apicontabilidad.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Generates HS256-signed JWT tokens for integration tests.
 * Secret must match app.jwt.secret in application-test.properties.
 */
public final class JwtTestHelper {

    static final String TEST_SECRET = "test-secret-minimum-32-chars-for-hmac";
    private static final long EXPIRATION_MS = 86_400_000L;

    private JwtTestHelper() {
    }

    public static String tokenFor(String username, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        List<String> roleList = Arrays.asList(roles);
        return Jwts.builder()
                .subject(username)
                .claim("roles", roleList)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }

    public static String adminToken() {
        return tokenFor("admin", "ROLE_ADMIN");
    }

    public static String contadorToken() {
        return tokenFor("contador1", "ROLE_CONTADOR");
    }

    public static String clienteToken() {
        return tokenFor("cliente1", "ROLE_CLIENTE");
    }
}
