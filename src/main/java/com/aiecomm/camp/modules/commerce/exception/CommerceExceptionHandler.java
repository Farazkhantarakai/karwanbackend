package com.aiecomm.camp.modules.commerce.exception;

import com.aiecomm.camp.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for commerce domain errors.
 * Maps CommerceException codes to appropriate HTTP status codes.
 */
@RestControllerAdvice(basePackages = "com.aiecomm.camp.modules.commerce")
@Slf4j
public class CommerceExceptionHandler {

    @ExceptionHandler(CommerceException.class)
    public ResponseEntity<ApiResponse<Object>> handleCommerceException(CommerceException ex) {
        log.warn("Commerce exception: code={} message={}", ex.getCode(), ex.getMessage());

        HttpStatus status = switch (ex.getCode()) {
            case "STORE_NOT_FOUND"          -> HttpStatus.BAD_REQUEST;
            case "PRODUCT_NOT_FOUND",
                 "CART_NOT_FOUND"           -> HttpStatus.NOT_FOUND;
            case "CROSS_STORE_ACCESS"       -> HttpStatus.FORBIDDEN;
            case "INSUFFICIENT_INVENTORY",
                 "INVALID_QUANTITY",
                 "PRODUCT_UNAVAILABLE"      -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "RATE_LIMITED"             -> HttpStatus.TOO_MANY_REQUESTS;
            default                         -> HttpStatus.BAD_REQUEST;
        };

        ApiResponse<Object> body = ApiResponse.<Object>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getCode())
                .data(ex.getDetails() != null ? ex.getDetails() : null)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Invalid request");

        return ResponseEntity.badRequest().body(ApiResponse.<Object>builder()
                .success(false)
                .message(firstError)
                .errorCode("INVALID_QUANTITY")
                .build());
    }
}
