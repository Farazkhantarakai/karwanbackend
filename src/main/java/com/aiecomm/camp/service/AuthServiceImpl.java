package com.aiecomm.camp.service;

import com.aiecomm.camp.dto.*;
import com.aiecomm.camp.entity.AuthProvider;
import com.aiecomm.camp.entity.Role;
import com.aiecomm.camp.entity.User;
import com.aiecomm.camp.repository.UserRepository;
import com.aiecomm.camp.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

        // ── GOOGLE TOKEN VERIFICATION & DATA EXTRACTION ──
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
            if (StringUtils.hasText(picture)) {
                user.setImageUrl(picture);
            }
            if (StringUtils.hasText(providerId)) {
                user.setProviderId(providerId);
            }
            user = userRepository.save(user);
        } else {
            user = User.builder()
                    .name(name)
                    .email(email)
                    .imageUrl(picture)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .role(Role.ROLE_USER)
                    .build();
            user = userRepository.save(user);
        }



        String accessToken = jwtUtils.generateAccessTokenFromEmail(user.getEmail());
        String refreshToken = jwtUtils.generateRefreshTokenFromEmail(user.getEmail());

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
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String email = jwtUtils.getUsernameFromJwtToken(refreshToken);
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        String newAccessToken = jwtUtils.generateAccessTokenFromEmail(email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
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
}

