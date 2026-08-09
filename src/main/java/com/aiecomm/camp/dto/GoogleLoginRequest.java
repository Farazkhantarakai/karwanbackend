package com.aiecomm.camp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    private String credential; // Google ID Token or Access Token from frontend @react-oauth/google
    private String idToken;    // Alternative field name for Google ID Token
    private String email;     // Provided by frontend if direct info exchange
    private String name;      // User display name from Google
    private String picture;   // User profile avatar URL
}
