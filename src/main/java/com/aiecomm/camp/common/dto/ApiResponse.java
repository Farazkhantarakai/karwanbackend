package com.aiecomm.camp.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {


    private boolean success;
    private String message;
    private T data;                      // Can be a single Object, a List<...>, or null!
    private String errorCode;
    private Boolean upgradeRequired;
    private LocalDateTime timestamp;
    // ── Static Helper Methods for Clean Controller Code ──
    // 1. Success with Data & Default Message
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    // 2. Success with Custom Message & Data
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    // 3. Success with only Message (No data payload, e.g. "Logout successful")
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    // 4. Error Response
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 5. Structured Error Response with code and upgradeRequired
    public static <T> ApiResponse<T> error(String message, String errorCode, Boolean upgradeRequired) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .upgradeRequired(upgradeRequired)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
