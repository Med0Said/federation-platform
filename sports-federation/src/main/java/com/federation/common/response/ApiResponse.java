package com.federation.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Unified API response envelope for all endpoints.
 *
 * Success:  { success: true,  data: T,    message: "...", timestamp: "..." }
 * Error:    { success: false, data: null, message: "...", status: 4xx/5xx, path: "..." }
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final Integer status;
    private final String path;
    private final Instant timestamp;

    // Private constructor — use static factories
    private ApiResponse(boolean success, T data, String message, Integer status, String path) {
        this.success   = success;
        this.data      = data;
        this.message   = message;
        this.status    = status;
        this.path      = path;
        this.timestamp = Instant.now();
    }

    // ----------------------------------------------------------------
    // Success factories
    // ----------------------------------------------------------------

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, data, "Resource created successfully", 201, null);
    }

    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, null, "Operation completed successfully", 204, null);
    }

    // ----------------------------------------------------------------
    // Error factories
    // ----------------------------------------------------------------

    public static <T> ApiResponse<T> error(int status, String message, String path) {
        return new ApiResponse<>(false, null, message, status, path);
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(false, null, message, status, null);
    }
}
