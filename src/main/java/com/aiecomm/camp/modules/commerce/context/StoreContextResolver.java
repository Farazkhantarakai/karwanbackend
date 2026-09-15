package com.aiecomm.camp.modules.commerce.context;

import com.aiecomm.camp.modules.commerce.exception.CommerceException;
import com.aiecomm.camp.modules.store.entity.Domain;
import com.aiecomm.camp.modules.store.entity.Settings;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.DomainRepository;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Resolves a StoreContext from the incoming HTTP request.
 *
 * Resolution order:
 *  1. Canonical host header -> Redis cache (DomainCacheService)
 *  2. Cache miss -> DomainRepository.findByName(host) -> DB
 *  3. Dev profile only: X-Store-Slug header fallback
 *
 * Production NEVER uses X-Store-Slug. It is strictly a dev/testing override.
 * On failure: throws CommerceException("STORE_NOT_FOUND").
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreContextResolver {

    private final DomainRepository domainRepository;
    private final StoreRepository storeRepository;
    private final DomainCacheService domainCacheService;
    private final Environment environment;

    @Value("${app-domain-suffix:.karwan.pk}")
    private String appDomainSuffix;

    public StoreContext resolve(HttpServletRequest request) {
        // X-Forwarded-Host is forwarded from Next.js Route Handler (trusted server-to-server)
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String rawHost = (forwardedHost != null && !forwardedHost.isBlank())
                ? forwardedHost
                : request.getServerName();

        String canonicalHost = HostCanonicalizer.canonicalize(rawHost);
        log.debug("StoreContextResolver: resolving host '{}'", canonicalHost);

        // 1. Cache
        Optional<StoreContext> cached = domainCacheService.getFromCache(canonicalHost);
        if (cached.isPresent()) {
            log.debug("StoreContextResolver: cache hit for '{}'", canonicalHost);
            return cached.get();
        }

        // 2. DB via domain name
        Optional<StoreContext> fromDomain = resolveByDomainName(canonicalHost);
        if (fromDomain.isPresent()) {
            domainCacheService.putInCache(canonicalHost, fromDomain.get());
            return fromDomain.get();
        }

        // 3. Dev profile only: X-Store-Slug fallback
        if (isDevProfile()) {
            String slug = request.getHeader("X-Store-Slug");
            if (slug != null && !slug.isBlank()) {
                log.debug("StoreContextResolver: dev fallback via X-Store-Slug='{}'", slug);
                return resolveBySlug(slug)
                        .orElseThrow(() -> new CommerceException("STORE_NOT_FOUND",
                                "No store found for slug: " + slug));
            }
        }

        throw new CommerceException("STORE_NOT_FOUND",
                "No store resolved for host: " + canonicalHost);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Optional<StoreContext> resolveByDomainName(String canonicalHost) {
        return domainRepository.findByName(canonicalHost)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(domain -> toStoreContext(domain.getStore()));
    }

    private Optional<StoreContext> resolveBySlug(String slug) {
        return storeRepository.findBySlug(slug)
                .map(this::toStoreContext);
    }

    private StoreContext toStoreContext(Store store) {
        String currency = "PKR";
        String country  = "PK";
        Settings settings = store.getSettings();
        if (settings != null) {
            if (settings.getCountryCurrency() != null) currency = settings.getCountryCurrency();
            if (settings.getCountryCode() != null)     country  = settings.getCountryCode();
        }
        return new StoreContext(
                store.getStoreId(),
                store.getTenant().getTenantId(),
                currency,
                country,
                store.getSlug()
        );
    }

    private boolean isDevProfile() {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) active = environment.getDefaultProfiles();
        return Arrays.stream(active).anyMatch(p -> p.equalsIgnoreCase("dev")
                || p.equalsIgnoreCase("default")
                || p.equalsIgnoreCase("local"));
    }
}
