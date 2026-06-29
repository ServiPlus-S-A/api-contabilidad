package com.serviplus.apicontabilidad.utility;

import java.time.LocalDateTime;
import java.time.ZoneId;

public record ApiError(
        int status,
        String message,
        LocalDateTime timestamp
) {
    public ApiError(int status, String message) {
        this(status, message, LocalDateTime.now(ZoneId.systemDefault()));
    }
}
