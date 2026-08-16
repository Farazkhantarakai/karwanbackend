package com.aiecomm.camp.modules.auth.service;

import com.aiecomm.camp.modules.auth.dto.*;
import com.aiecomm.camp.modules.auth.entity.AuthProvider;
import com.aiecomm.camp.modules.user.entity.Role;
import com.aiecomm.camp.modules.user.entity.User;
import com.aiecomm.camp.modules.user.dto.UserDto;
import com.aiecomm.camp.modules.user.repository.UserRepository;
import com.aiecomm.camp.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * Saves active access & refresh tokens into Redis with TTL matching their expiration.
     */
    private void saveActiveTokens(String email, String accessToken, String refreshToken) {
        try {
            if (StringUtils.hasText(accessToken)) {
                String cleanAccess = jwtUtils.cleanToken(accessToken);
                java.time.Instant exp = jwtUtils.getExpirationDateFromJwtToken(cleanAccess);
                if (exp != null) {
                    Duration ttl = Duration.between(java.time.Instant.now(), exp);
                    if (!ttl.isNegative() && !ttl.isZero()) {
                        redisTemplate.opsForValue().set("auth:token:access:" + cleanAccess, email, ttl);
                        redisTemplate.opsForValue().set("auth:user:active:" + email, cleanAccess, ttl);
                    }
                }
            }

            if (StringUtils.hasText(refreshToken)) {
                String cleanRefresh = jwtUtils.cleanToken(refreshToken);
                java.time.Instant exp = jwtUtils.getExpirationDateFromJwtToken(cleanRefresh);
                if (exp != null) {
                    Duration ttl = Duration.between(java.time.Instant.now(), exp);
                    if (!ttl.isNegative() && !ttl.isZero()) {
                        redisTemplate.opsForValue().set("auth:token:refresh:" + cleanRefresh, email, ttl);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to store active token in Redis: " + e.getMessage());
        }
    }

    /**
     * Blacklists a JWT token in Redis for its remaining validity duration (or 7-day fallback TTL).
     */
    private void blacklistToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) return;
        String token = jwtUtils.cleanToken(rawToken);
        if (!StringUtils.hasText(token)) return;

        try {
            java.time.Instant exp = jwtUtils.getExpirationDateFromJwtToken(token);
            Duration remaining = Duration.ofDays(7); // Fallback TTL
            if (exp != null) {
                Duration calc = Duration.between(java.time.Instant.now(), exp);
                if (!calc.isNegative() && !calc.isZero()) {
                    remaining = calc;
                }
            }
            redisTemplate.opsForValue().set("auth:blacklist:" + token, "REVOKED", remaining);
            redisTemplate.delete("auth:token:access:" + token);
            redisTemplate.delete("auth:token:refresh:" + token);
        } catch (Exception e) {
            System.err.println("Warning: Failed to blacklist token in Redis: " + e.getMessage());
        }
    }

    /**
     * Checks if a token is present in the Redis blacklist.
     */
    public boolean isBlacklisted(String rawToken) {
        String token = jwtUtils.cleanToken(rawToken);
        if (!StringUtils.hasText(token)) return false;

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("auth:blacklist:" + token));
        } catch (Exception e) {
            System.err.println("Warning: Failed to check token blacklist in Redis: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.getProvider() == AuthProvider.GOOGLE) {
                throw new IllegalArgumentException("An account already exists with Google Sign-In for " + email + ". Please click 'Login with Google'.");
            } else {
                throw new IllegalArgumentException("Email address is already in use: " + email + ". Please log in instead.");
            }
        }

        User user = User.builder()
                .name(request.getName() != null ? request.getName().trim() : "Merchant")
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtUtils.generateAccessTokenFromEmail(savedUser.getEmail());
        String refreshToken = jwtUtils.generateRefreshTokenFromEmail(savedUser.getEmail());
        saveActiveTokens(savedUser.getEmail(), accessToken, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getJwtExpirationMs() / 1000)
                .user(UserDto.fromEntity(savedUser))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getProvider() == AuthProvider.GOOGLE && !StringUtils.hasText(user.getPassword())) {
                throw new IllegalArgumentException("This account was created using Google Sign-In. Please click 'Login with Google'.");
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            if (userOptional.isPresent() && userOptional.get().getProvider() == AuthProvider.GOOGLE) {
                throw new IllegalArgumentException("This account was created using Google Sign-In. Please click 'Login with Google'.");
            }
            throw new BadCredentialsException("Invalid email or password.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshTokenFromEmail(user.getEmail());
        saveActiveTokens(user.getEmail(), accessToken, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getJwtExpirationMs() / 1000)
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        String email = request.getEmail();
        String name = request.getName();
        String picture = request.getPicture();
        String token = request.getCredential() != null ? request.getCredential() : request.getIdToken();

        String providerId = null;

        if (StringUtils.hasText(token)) {
            Map<String, Object> googleProfile = fetchGoogleProfile(token);
            if (googleProfile != null) {
                if (googleProfile.get("email") != null) {
                    email = (String) googleProfile.get("email");
                }
                if (googleProfile.get("name") != null) {
                    name = (String) googleProfile.get("name");
                }
                if (googleProfile.get("picture") != null) {
                    picture = (String) googleProfile.get("picture");
                }
                if (googleProfile.get("sub") != null) {
                    providerId = (String) googleProfile.get("sub");
                } else if (googleProfile.get("user_id") != null) {
                    providerId = (String) googleProfile.get("user_id");
                }
            }
        }

        if (!StringUtils.hasText(providerId) && StringUtils.hasText(token)) {
            providerId = token.length() > 250 ? token.substring(0, 250) : token;
        }

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Google authentication failed: Email not provided by Google.");
        }

        email = email.trim().toLowerCase();
        if (!StringUtils.hasText(name)) {
            name = "Google User";
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getProvider() == AuthProvider.LOCAL) {
                throw new IllegalArgumentException("An account already exists with email " + email + " using Email/Password. Please log in using your email and password.");
            }
            if (StringUtils.hasText(providerId)) {
                user.setProviderId(providerId);
            }
            user = userRepository.save(user);
        } else {
            user = User.builder()
                    .name(name)
                    .email(email)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .role(Role.ROLE_USER)
                    .build();
            user = userRepository.save(user);
        }

        String accessToken = jwtUtils.generateAccessTokenFromEmail(user.getEmail());
        String refreshToken = jwtUtils.generateRefreshTokenFromEmail(user.getEmail());
        saveActiveTokens(user.getEmail(), accessToken, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getJwtExpirationMs() / 1000)
                .user(UserDto.fromEntity(user))
                .build();
    }

    private Map<String, Object> fetchGoogleProfile(String token) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + token;
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
                return restTemplate.getForObject(url, Map.class);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!StringUtils.hasText(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is required.");
        }

        String cleanRefresh = jwtUtils.cleanToken(refreshToken);

        if (!jwtUtils.validateJwtToken(cleanRefresh)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        if (isBlacklisted(cleanRefresh)) {
            throw new IllegalArgumentException("Refresh token has been revoked. Please log in again.");
        }

        String email = jwtUtils.getUsernameFromJwtToken(cleanRefresh);
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        String newAccessToken = jwtUtils.generateAccessTokenFromEmail(email);
        saveActiveTokens(email, newAccessToken, null);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(cleanRefresh)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getJwtExpirationMs() / 1000)
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Override
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        String email = null;

        if (StringUtils.hasText(accessToken)) {
            String cleanAccess = jwtUtils.cleanToken(accessToken);
            blacklistToken(cleanAccess);
            try {
                email = jwtUtils.getUsernameFromJwtToken(cleanAccess);
            } catch (Exception ignored) {}
        }

        if (StringUtils.hasText(refreshToken)) {
            String cleanRefresh = jwtUtils.cleanToken(refreshToken);
            blacklistToken(cleanRefresh);
            if (!StringUtils.hasText(email)) {
                try {
                    email = jwtUtils.getUsernameFromJwtToken(cleanRefresh);
                } catch (Exception ignored) {}
            }
        }

        if (StringUtils.hasText(email)) {
            try {
                redisTemplate.delete("auth:user:active:" + email.trim().toLowerCase());
            } catch (Exception ignored) {}
        }
    }
}
