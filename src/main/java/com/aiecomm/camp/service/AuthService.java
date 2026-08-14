package com.aiecomm.camp.service;

import com.aiecomm.camp.dto.*;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    UserDto getCurrentUser(String email);

    void logout(String accessToken, String refreshToken);
}

