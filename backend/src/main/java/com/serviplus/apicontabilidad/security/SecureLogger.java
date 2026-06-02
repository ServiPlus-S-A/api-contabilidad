package com.serviplus.apicontabilidad.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <<Decorator Pattern>> — Wraps Slf4j with sanitized output to prevent
 * log injection attacks (OWASP A09). Strips CR/LF from user-controlled
 * values before writing to the log.
 */
public final class SecureLogger {

    private static final Logger log = LoggerFactory.getLogger(SecureLogger.class);

    private SecureLogger() {
    }

    public static void logAuthFailure(String path, String reason) {
        log.warn("Auth failure [{}]: {}", sanitize(path), sanitize(reason));
    }

    public static void logAccessDenied(String username, String path) {
        log.warn("Access denied — user=[{}] path=[{}]", sanitize(username), sanitize(path));
    }

    private static String sanitize(String value) {
        if (value == null) return "null";
        return value.replaceAll("[\r\n\t]", "_");
    }
}
