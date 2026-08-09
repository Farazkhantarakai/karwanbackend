package com.aiecomm.camp.service;

import com.aiecomm.camp.dto.*;
import com.aiecomm.camp.entity.AuthProvider;
import com.aiecomm.camp.entity.Role;
import com.aiecomm.camp.entity.User;
import com.aiecomm.camp.repository.UserRepository;
import com.aiecomm.camp.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email address is already in use: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.getEmail()));

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
        String name = StringUtils.hasText(request.getName()) ? request.getName() : "Google User";
        String picture = request.getPicture();

        // If email is not directly passed, default or parse from credential
        if (!StringUtils.hasText(email)) {
            email = "google.user@karwan.pk";
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update user picture or provider details if needed
            if (StringUtils.hasText(picture)) {
                user.setImageUrl(picture);
                userRepository.save(user);
            }
        } else {
            user = User.builder()
                    .name(name)
                    .email(email)
                    .imageUrl(picture)
                    .provider(AuthProvider.GOOGLE)
                    .providerId(request.getCredential() != null ? request.getCredential() : request.getIdToken())
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

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String email = jwtUtils.getUsernameFromJwtToken(refreshToken);
        User user = userRepository.findByEmail(email)
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return UserDto.fromEntity(user);
    }
}
