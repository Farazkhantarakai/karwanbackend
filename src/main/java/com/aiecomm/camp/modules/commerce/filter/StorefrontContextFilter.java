package com.aiecomm.camp.modules.commerce.filter;

import com.aiecomm.camp.modules.commerce.context.*;
import com.aiecomm.camp.modules.commerce.exception.CommerceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Storefront filter that runs on every /api/storefront/** request.
 *
 * Responsibilities:
 *  1. Resolve StoreContext from host (cached Redis -> DB) via StoreContextResolver
 *  2. Read X-Cart-Token header (forwarded by Next.js Route Handler — never from browser)
 *  3. Hash the raw token: SHA-256(rawToken)
 *  4. Build CommerceContext and set on CommerceContextHolder
 *  5. Always clear ThreadLocals in finally block
 *
 * Cookie management is NOT here — Next.js Route Handler owns the browser cookie.
 * Spring only reads X-Cart-Token header from trusted server-to-server calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StorefrontContextFilter extends OncePerRequestFilter {

    private final StoreContextResolver storeContextResolver;
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
        try {
            // Step 1: Resolve StoreContext (cache-first)
            StoreContext storeCtx = storeContextResolver.resolve(request);
            StoreContextHolder.set(storeCtx);

            // Step 2 & 3: X-Cart-Token header -> SHA-256 hash
            // This header is set by Next.js Route Handler (server-to-server, trusted)
            String rawToken = request.getHeader("X-Cart-Token");
            String tokenHash = (rawToken != null && !rawToken.isBlank())
                    ? sha256(rawToken)
                    : null;

            // Step 4: Build CommerceContext
            CommerceContext ctx = new CommerceContext(storeCtx, tokenHash, null);
            CommerceContextHolder.set(ctx);

            log.debug("CommerceContext set: storeId={} hasToken={}",
                    storeCtx.storeId(), tokenHash != null);

            chain.doFilter(request, response);

        } catch (CommerceException e) {
            // Store not found or other commerce pre-conditions failed
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "success", false,
                    "code", e.getCode(),
                    "message", e.getMessage()
            ));
        } finally {
            // Step 5: Always clear ThreadLocals — prevents context leakage between requests
            StoreContextHolder.clear();
            CommerceContextHolder.clear();
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
