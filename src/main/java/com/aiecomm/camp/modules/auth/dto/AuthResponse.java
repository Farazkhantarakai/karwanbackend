package com.aiecomm.camp.modules.auth.dto;

import com.aiecomm.camp.modules.user.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresInSeconds;
    private UserDto user;

    @JsonProperty("isNewUser")
    private Boolean isNewUser;

    @Builder.Default
    private Boolean success = true;
}
