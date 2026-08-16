package com.aiecomm.camp.modules.auth.controller;

import com.aiecomm.camp.modules.auth.dto.*;
import com.aiecomm.camp.modules.auth.service.AuthService;
import com.aiecomm.camp.modules.user.dto.UserDto;
import com.aiecomm.camp.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
        return new ResponseEntity<>(response, HttpStatus.CREATED);
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
}
