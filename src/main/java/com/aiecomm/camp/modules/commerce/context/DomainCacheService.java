package com.aiecomm.camp.modules.commerce.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed cache for domain -> StoreContext lookups.
 *
 * Key pattern:  "domain:{canonicalHost}"
 * TTL:          10 minutes (domain-to-store mappings change infrequently)
 *
 * Cache invalidation: call evict(host) whenever a Domain entity is created,
 * updated, or deleted (hook into DomainService / StoreService).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DomainCacheService {

    private static final String KEY_PREFIX = "domain:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<StoreContext> getFromCache(String canonicalHost) {
        try {
            String key = KEY_PREFIX + canonicalHost;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, StoreContext.class));
        } catch (Exception e) {
            log.warn("Domain cache read error for host '{}': {}", canonicalHost, e.getMessage());
            return Optional.empty();
        }
    }

    public void putInCache(String canonicalHost, StoreContext ctx) {
        try {
            String key = KEY_PREFIX + canonicalHost;
            String json = objectMapper.writeValueAsString(ctx);
            redisTemplate.opsForValue().set(key, json, TTL);
        } catch (Exception e) {
            log.warn("Domain cache write error for host '{}': {}", canonicalHost, e.getMessage());
            // Non-fatal: miss on next request, will re-query DB
        }
    }

    /**
     * Evict a host from cache. Call this when a domain mapping changes.
     */
    public void evict(String canonicalHost) {
        redisTemplate.delete(KEY_PREFIX + canonicalHost);
        log.debug("Domain cache evicted for host '{}'", canonicalHost);
    }
}
