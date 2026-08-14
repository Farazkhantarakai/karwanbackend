package com.aiecomm.camp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom JWT Authentication Filter extending OncePerRequestFilter.
 * Guarantees a single execution per request dispatch in a single request thread.
 * 
 * Logic flow:
 * 1. Extract Bearer token from the 'Authorization' HTTP Header.
 * 2. Validate JWT signature and expiration.
 * 3. Extract user email/username from JWT claims.
 * 4. Load UserDetails from DB and populate SecurityContextHolder.
 * 
 * You can modify or extend the custom validation rules in doFilterInternal as needed.
 */
import java.time.Instant;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtUtils jwtUtils;


    private final RedisTemplate<String,String> redisTemplate;


    private final CustomUserDetailsService customUserDetailsService;

    JwtAuthenticationFilter(JwtUtils jwtUtils,RedisTemplate<String,String> redisTemplate,CustomUserDetailsService customUserDetailsService){
        this.jwtUtils=jwtUtils;
        this.redisTemplate=redisTemplate;
        this.customUserDetailsService=customUserDetailsService;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Extract JWT token from header
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // 2. Redis Blacklist Check
                boolean isBlacklisted = false;
                try {
                    isBlacklisted = Boolean.TRUE.equals(redisTemplate.hasKey("auth:blacklist:" + jwt));
                } catch (Exception ex) {
                    logger.warn("Could not check Redis blacklist for token: " + ex.getMessage());
                }

                if (isBlacklisted) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"success\":false,\"error\":\"Token has been revoked. Please log in again.\"}");
                    return;
                }

                String username = jwtUtils.getUsernameFromJwtToken(jwt);

                // Load user details
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication in SecurityContext: {}", e);
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }



    /**
     * Helper method to extract Bearer JWT token from HTTP request header.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}
