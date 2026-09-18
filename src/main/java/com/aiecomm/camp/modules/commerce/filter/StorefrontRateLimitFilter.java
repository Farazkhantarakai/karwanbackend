package com.aiecomm.camp.modules.commerce.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed fixed-window rate limiter for public storefront endpoints.
 * Runs before StorefrontContextFilter to reject abusive traffic cheaply.
 *
 * Limits (per IP per 60-second window):
 *   GET  /api/storefront/cart         60/min
 *   POST /api/storefront/cart/items   20/min
 *   PATCH|DELETE /api/storefront/**   20/min
 *
 * Redis key pattern: "rl:storefront:{method}:{path-bucket}:{client-ip}"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorefrontRateLimitFilter extends OncePerRequestFilter {

    private static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/storefront/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri    = request.getRequestURI();
        String ip     = resolveClientIp(request);
        int limit     = resolveLimit(method, uri);

        String bucket = resolveBucket(method, uri);
        String key    = "rl:storefront:" + bucket + ":" + ip;

        Long count = null;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Rate limit Redis error (failing open): {}", e.getMessage());
            // Fail open: don't block legitimate users if Redis has a transient error
        }

        if (count != null && count > limit) {
            log.warn("Rate limit exceeded: ip={} key={} count={}", ip, key, count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "success", false,
                    "code", "RATE_LIMITED",
                    "message", "Too many requests. Please slow down.",
                    "retryAfterSeconds", WINDOW_SECONDS
            ));
            return;
        }

        chain.doFilter(request, response);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private int resolveLimit(String method, String uri) {
        return switch (method.toUpperCase()) {
            case "GET"    -> 60;
            case "POST"   -> 20;
            case "PATCH"  -> 20;
            case "DELETE" -> 30;
            default       -> 40;
        };
    }

    private String resolveBucket(String method, String uri) {
        // Normalize path to a stable bucket key (avoid per-ID keys flooding Redis)
        String base = uri.replaceAll("/[0-9a-fA-F-]{8,}", "/{id}");
        return method.toLowerCase() + ":" + base.hashCode();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Trust X-Forwarded-For from proxy (Next.js server forwards it)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
