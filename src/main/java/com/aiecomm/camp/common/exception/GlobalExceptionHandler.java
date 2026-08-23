package com.aiecomm.camp.common.exception;

import com.aiecomm.camp.modules.auth.exception.TenantUserAlreadyExists;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("code", ex.getCode() != null ? ex.getCode() : "EMAIL_ALREADY_EXISTS");
        body.put("error", ex.getError() != null ? ex.getError() : "Email address is already in use.");
        body.put("message", ex.getMessage());
        body.put("provider", ex.getProvider() != null ? ex.getProvider() : "LOCAL");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(TenantUserAlreadyExists.class)
    public ResponseEntity<Map<String,Object>>  handleEntityAlreadyExistsForUser(TenantUserAlreadyExists ex){

        HashMap<String,Object> bucket=new HashMap<>();

        bucket.put("Status",HttpStatus.CONFLICT);
        bucket.put("message",ex.getMessage());

        return  ResponseEntity.status(HttpStatus.CONFLICT).body(bucket);
    }


    @ExceptionHandler(AccountExistsWithOAuthException.class)
    public ResponseEntity<Map<String, Object>> handleAccountExistsWithOAuthException(AccountExistsWithOAuthException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("code", ex.getCode() != null ? ex.getCode() : "ACCOUNT_EXISTS_WITH_GOOGLE");
        body.put("error", ex.getError() != null ? ex.getError() : "Account exists with Google Sign-In");
        body.put("message", ex.getMessage());
        body.put("provider", ex.getProvider() != null ? ex.getProvider() : "GOOGLE");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("success", false);
        body.put("error", ex.getMessage() != null && !ex.getMessage().equalsIgnoreCase("Bad credentials") 
                ? ex.getMessage() 
                : "Invalid email or password.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleOAuth2AuthenticationException(org.springframework.security.oauth2.core.OAuth2AuthenticationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", ex.getMessage());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", ex.getMessage());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        body.put("error", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
