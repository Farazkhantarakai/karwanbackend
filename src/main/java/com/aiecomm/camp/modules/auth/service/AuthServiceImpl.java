package com.aiecomm.camp.modules.auth.service;

import com.aiecomm.camp.NotificationService.BrevoNotificationImpl;
import com.aiecomm.camp.NotificationService.NotifcationRequest;
import com.aiecomm.camp.NotificationService.NotificationCache;
import com.aiecomm.camp.NotificationService.NotificationResponse;
import com.aiecomm.camp.NotificationService.dto.BrevoResponse;
import com.aiecomm.camp.NotificationService.dto.BrevoTemplate;
import com.aiecomm.camp.NotificationService.dto.Reciever;
import com.aiecomm.camp.NotificationService.dto.Sender;
import com.aiecomm.camp.NotificationService.entity.PlatformTemplate;
import com.aiecomm.camp.NotificationService.enums.NotificationChannel;
import com.aiecomm.camp.NotificationService.enums.NotificationEvent;
import com.aiecomm.camp.common.exception.AccountExistsWithOAuthException;
import com.aiecomm.camp.common.exception.EmailAlreadyExistsException;
import com.aiecomm.camp.modules.auth.dto.*;
import com.aiecomm.camp.modules.auth.entity.AuthProvider;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import com.aiecomm.camp.modules.tenant.entity.TenantUser;
import com.aiecomm.camp.modules.tenant.serviceimpl.TenantServiceImpl;
import com.aiecomm.camp.modules.user.entity.Role;
import com.aiecomm.camp.modules.user.entity.User;
import com.aiecomm.camp.modules.user.dto.UserDto;
import com.aiecomm.camp.modules.user.repository.UserRepository;
import com.aiecomm.camp.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.random.RandomGenerator;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TenantServiceImpl tenantServiceImp;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private NotificationCache notificationCache;

    @Autowired
    private BrevoNotificationImpl brevoNotificationImpl;


    @Value("${platform-mail}")
    private    String platformMail;
    @Value("${platform-name}")
    private    String platformName;



    private Logger logger= LoggerFactory.getLogger(AuthServiceImpl.class);

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
                throw new AccountExistsWithOAuthException(
                        "ACCOUNT_EXISTS_WITH_GOOGLE",
                        "Account exists with Google Sign-In",
                        "An account already exists with Google Sign-In for " + email + ". Please sign in with Google.",
                        "GOOGLE"
                );
            } else {
                throw new EmailAlreadyExistsException(
                        "EMAIL_ALREADY_EXISTS",
                        "Email address is already in use.",
                        "An account with this email already exists: " + email + ". Please log in instead.",
                        "LOCAL"
                );
            }
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
     UUID tenantId=tenantServiceImp.createTenantForUser(savedUser);


        String accessToken = jwtUtils.generateAccessTokenFromEmail(savedUser.getEmail(),tenantId);
        String refreshToken = jwtUtils.generateRefreshTokenFromEmail(savedUser.getEmail(),tenantId);
        saveActiveTokens(savedUser.getEmail(), accessToken, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getJwtExpirationMs() / 1000)
                .user(UserDto.fromEntity(savedUser))
                .isNewUser(true)
                .success(true)
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
                throw new AccountExistsWithOAuthException(
                        "ACCOUNT_EXISTS_WITH_GOOGLE",
                        "Account exists with Google Sign-In",
                        "This account was created using Google Sign-In. Please sign in with Google.",
                        "GOOGLE"
                );
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            if (userOptional.isPresent() && userOptional.get().getProvider() == AuthProvider.GOOGLE) {
                throw new AccountExistsWithOAuthException(
                        "ACCOUNT_EXISTS_WITH_GOOGLE",
                        "Account exists with Google Sign-In",
                        "This account was created using Google Sign-In. Please sign in with Google.",
                        "GOOGLE"
                );
            }
            throw new BadCredentialsException("Invalid email or password.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        UUID tenantId=tenantServiceImp.getUserTenantId(user);

        String accessToken = jwtUtils.generateAccessToken(authentication,tenantId);
        String refreshToken = jwtUtils.generateRefreshTokenFromEmail(user.getEmail(),tenantId);
        saveActiveTokens(user.getEmail(), accessToken, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getJwtExpirationMs() / 1000)
                .user(UserDto.fromEntity(user))
                .isNewUser(false)
                .success(true)
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
        boolean isNewUser = false;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getProvider() == AuthProvider.LOCAL) {
                throw new EmailAlreadyExistsException(
                        "EMAIL_ALREADY_EXISTS",
                        "Email address is already in use with Password login.",
                        "An account already exists with email " + email + " using Email/Password. Please log in using your email and password.",
                        "LOCAL"
                );
            }
            if (StringUtils.hasText(providerId)) {
                user.setProviderId(providerId);
            }
            user = userRepository.save(user);
        } else {
            isNewUser = true;
            user = User.builder()
                    .name(name)
                    .email(email)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .role(Role.ROLE_USER)
                    .build();
            user = userRepository.save(user);
        }

     UUID tenantId =tenantServiceImp.createTenantForUser(user);


        String accessToken = jwtUtils.generateAccessTokenFromEmail(user.getEmail(),tenantId);

        String refreshToken = jwtUtils.generateRefreshTokenFromEmail(user.getEmail(),tenantId);
        saveActiveTokens(user.getEmail(), accessToken, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getJwtExpirationMs() / 1000)
                .user(UserDto.fromEntity(user))
                .isNewUser(isNewUser)
                .success(true)
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

       UUID tenantId= tenantServiceImp.getUserTenantId(user);

        String newAccessToken = jwtUtils.generateAccessTokenFromEmail(email,tenantId);
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

    @Override
    public void forgotPassword(UserDto userDto) {
        if (userDto == null || !StringUtils.hasText(userDto.getEmail())) {
            throw new IllegalArgumentException("User email is required");
        }
        String otp = generateOtp();
        String normalizedEmail = userDto.getEmail().trim().toLowerCase();

        PlatformTemplate template = notificationCache.getTemplate(NotificationEvent.PASSWORD_RESET, NotificationChannel.EMAIL);
        if (template == null) {
            logger.error("No template found for PASSWORD_RESET event in cache.");
            throw new IllegalStateException("Email notification template is not configured. Please contact support.");
        }

        String context = template.getTemplateBody() != null
                ? template.getTemplateBody().replace("${OTP}", otp)
                : "Your verification code is: " + otp;

        NotifcationRequest brevoTemplate = BrevoTemplate.builder()
                .sender(Sender.builder().email(platformMail).name(platformName).build())
                .to(Arrays.asList(Reciever.builder().email(normalizedEmail).name(userDto.getName()).build()))
                .subject(template.getTemplateSubject() != null ? template.getTemplateSubject() : "Password Reset OTP")
                .textContent(context)
                .build();

        NotificationResponse response = null;
        try {
            response = brevoNotificationImpl.sendMail(brevoTemplate);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to " + normalizedEmail, e);
            throw new IllegalStateException("Email delivery service is currently unavailable. Please try again later.");
        }

        if (response instanceof BrevoResponse && ((BrevoResponse) response).messageId() != null) {
            // Email successfully sent -> Store OTP in Redis with 2-minute TTL
            redisTemplate.opsForValue().set("otp:user:" + normalizedEmail, otp, Duration.ofMinutes(2));
            logger.info("Password reset OTP successfully sent and cached for user: " + normalizedEmail);
        } else {
            logger.error("Email service did not return a valid messageId for " + normalizedEmail);
            throw new IllegalStateException("Failed to deliver verification code to your email. Please try again later.");
        }
    }

    @Override
    public boolean checkOtp(String email, String otp) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(otp)) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase();
        String key = "otp:user:" + normalizedEmail;
        Object cachedOtp = redisTemplate.opsForValue().get(key);
        if (cachedOtp == null) {
            return false;
        }
        return cachedOtp.toString().trim().equals(otp.trim());
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(otp) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("Email, OTP, and new password are required");
        }
        String normalizedEmail = email.trim().toLowerCase();
        boolean isValidOtp = checkOtp(normalizedEmail, otp);
        if (!isValidOtp) {
            throw new IllegalArgumentException("Invalid or expired OTP verification code");
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + normalizedEmail));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Purge OTP and active session tokens from Redis
        redisTemplate.delete("otp:user:" + normalizedEmail);
        redisTemplate.delete("auth:user:active:" + normalizedEmail);
    }

    public String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        // Generates a random number strictly between 100000 and 999999
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
}
