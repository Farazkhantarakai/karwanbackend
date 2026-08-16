package com.aiecomm.camp.modules.auth.service;

import com.aiecomm.camp.modules.auth.dto.*;
import com.aiecomm.camp.modules.user.dto.UserDto;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    UserDto getCurrentUser(String email);

    void logout(String accessToken, String refreshToken);
}
