package com.aiecomm.camp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long jwtRefreshExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Authentication authentication,UUID tenantId) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateTokenFromUsername(userPrincipal.getUsername(), jwtExpirationMs,tenantId);
    }

    public String generateAccessTokenFromEmail(String email, UUID tenantId) {
        return generateTokenFromUsername(email, jwtExpirationMs,tenantId);
    }

    public String generateRefreshTokenFromEmail(String email,UUID tenantId) {
        return generateTokenFromUsername(email, jwtRefreshExpirationMs,tenantId);
    }

    public String generateTokenFromUsername(String username, long expirationMs,UUID tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        HashMap<String,Object> map=new LinkedHashMap<>();
        map.put("username",username);
        map.put("tenantId",tenantId);

        return Jwts.builder()
                .claims(map)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromJwtToken(String token) {
        String cleanToken = cleanToken(token);
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(cleanToken)
                .getPayload();

        return claims.getSubject();
    }

    public java.time.Instant getExpirationDateFromJwtToken(String token) {
        String cleanToken = cleanToken(token);
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(cleanToken)
                .getPayload();

        return claims.getExpiration().toInstant();
    }

    public java.time.Instant getIssuedAtFromJwtToken(String token) {
        String cleanToken = cleanToken(token);
        if (cleanToken == null) return null;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(cleanToken)
                    .getPayload();
            Date issuedAt = claims.getIssuedAt();
            return issuedAt != null ? issuedAt.toInstant() : null;
        } catch (Exception e) {
            return null;
        }
    }


    public boolean validateJwtToken(String authToken) {
        String cleanToken = cleanToken(authToken);
        if (cleanToken == null) return false;
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(cleanToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid JWT signature, expired, malformed, or unsupported
            return false;
        }
    }

    public String cleanToken(String token) {
        if (token == null) return null;
        String trimmed = token.trim();
        if (trimmed.startsWith("Bearer ")) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public UUID getTenantIdFromToken(String jwt) {
        String token = cleanToken(jwt);
        try {
            Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
            Object tenantIdObj = claims.get("tenantId");
            if (tenantIdObj == null) {
                return null;
            }
            if (tenantIdObj instanceof UUID) {
                return (UUID) tenantIdObj;
            }
            return UUID.fromString(tenantIdObj.toString());
        } catch (Exception e) {

            return null;
        }
    }
}

