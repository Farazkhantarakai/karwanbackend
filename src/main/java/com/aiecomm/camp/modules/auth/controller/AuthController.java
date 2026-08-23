package com.aiecomm.camp.modules.auth.controller;

import com.aiecomm.camp.modules.auth.dto.*;
import com.aiecomm.camp.modules.auth.service.AuthService;
import com.aiecomm.camp.modules.user.dto.UserDto;
import com.aiecomm.camp.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * User Signup Endpoint
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        AuthResponse response = authService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * User Login Endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Google OAuth2 Login & Signup Endpoint
     * POST /api/auth/google
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest googleLoginRequest) {
        AuthResponse response = authService.googleLogin(googleLoginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Access Token Refresh Endpoint
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        AuthResponse response = authService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Get Current Authenticated User Profile
     * GET /api/auth/me
     * Protected endpoint verified via JwtAuthenticationFilter
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto userDto = authService.getCurrentUser(userPrincipal.getEmail());
        return ResponseEntity.ok(userDto);
    }

    /**
     * User Logout & Token Blacklist Endpoint
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<java.util.Map<String, Object>> logout(
            jakarta.servlet.http.HttpServletRequest httpRequest,
            @RequestBody(required = false) LogoutRequest logoutRequest
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        String refreshToken = logoutRequest != null ? logoutRequest.getRefreshToken() : null;

        authService.logout(authHeader, refreshToken);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "Successfully logged out. Tokens have been blacklisted.");
        return ResponseEntity.ok(response);
    }


    /**
     * Send Forgot Password OTP
     * POST /api/auth/forgot
     */
    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        UserDto userDto = authService.getCurrentUser(request.getEmail());
        if (userDto != null) {
            authService.forgotPassword(userDto);
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "If an account with this email exists, a verification code has been sent.");
        return ResponseEntity.ok(response);
    }

    /**
     * Verify OTP from Redis
     * POST /api/auth/checkOtp
     */
    @PostMapping("/checkOtp")
    public ResponseEntity<?> checkOtp(@Valid @RequestBody CheckOtpRequest request) {
        boolean isExists = authService.checkOtp(request.getEmail(), request.getOtp());

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("isExists", isExists);

        if (isExists) {
            response.put("success", true);
            response.put("message", "OTP verified successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Invalid or expired verification code. Please request a new code.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Reset Password using OTP
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", "Password has been successfully updated.");
        return ResponseEntity.ok(response);
    }
}
