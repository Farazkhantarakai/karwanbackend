package com.aiecomm.camp.modules.store.controller;

import com.aiecomm.camp.common.dto.ApiResponse;
import com.aiecomm.camp.core.TenantContext;
import com.aiecomm.camp.modules.store.dto.DomainDto;
import com.aiecomm.camp.modules.store.dto.StoreDto;
import com.aiecomm.camp.modules.store.service.StoreService;
import com.aiecomm.camp.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.aiecomm.camp.security.JwtAuthenticationFilter.parseJwt;

@RestController
@RequestMapping({"/api/v1", "/api"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class StoreController {

    private final StoreService storeService;
    private final JwtUtils jwtUtils;

    /**
     * Get all stores belonging to the authenticated tenant.
     * Accessible via GET /api/v1/store or /api/v1/stores (also /api/store or /api/stores).
     */
    @GetMapping({"/store", "/stores"})
    public ApiResponse<List<StoreDto>> getStoresForUser(HttpServletRequest request) {
        log.info("REST: GET stores for user");
        UUID tenantId = resolveTenantId(request);
        if (tenantId == null) {
            return ApiResponse.error("Invalid token or tenant");
        }

        List<StoreDto> stores = storeService.getStoreStatusForUser(tenantId);
        return ApiResponse.success("Stores retrieved successfully", stores);
    }

    /**
     * Get a specific store by storeId with tenant isolation.
     * Accessible via GET /api/v1/store/{storeId} or /api/v1/stores/{storeId}.
     */
    @GetMapping({"/store/{storeId}", "/stores/{storeId}"})
    public ApiResponse<StoreDto> getStoreById(@PathVariable Long storeId, HttpServletRequest request) {
        log.info("REST: GET store by id: {}", storeId);
        ensureTenantContext(request);

        StoreDto store = storeService.getStoreForUser(storeId);
        return ApiResponse.success("Store retrieved successfully", store);
    }

    /**
     * Create a new store against the tenant.
     * Accessible via POST /api/v1/store or /api/v1/stores.
     */
    @PostMapping({"/store", "/stores"})
    public ApiResponse<StoreDto> createStoreAgainstTheTenant(@RequestBody StoreDto storeDto, HttpServletRequest request) {
        log.info("REST: POST create store: {}", storeDto.getStorename());
        UUID tenantId = storeDto.getTenantId();

        // Fallback: If tenantId is not in request body, resolve from context / JWT
        if (tenantId == null) {
            tenantId = resolveTenantId(request);
        }

        if (tenantId == null) {
            return ApiResponse.error("Tenant ID is required to create a store");
        }

        StoreDto store = storeService.createStoreAgainstTheTenant(
                tenantId,
                storeDto.getDomainname(),
                storeDto.getStorename()
        );

        if (store != null) {
            return ApiResponse.success("Store created successfully", store);
        }

        return ApiResponse.error("Something went wrong while creating store");
    }

    /**
     * Get domains for a store.
     * Accessible via GET /api/v1/domains?storeId=... or /api/v1/stores/{storeId}/domains.
     */
    @GetMapping({"/domains", "/stores/{storeId}/domains"})
    public ApiResponse<List<DomainDto>> getStoreDomains(
            @RequestParam(value = "storeId", required = false) Long queryStoreId,
            @PathVariable(value = "storeId", required = false) Long pathStoreId) {
        Long storeId = pathStoreId != null ? pathStoreId : queryStoreId;
        if (storeId == null) {
            return ApiResponse.error("storeId is required");
        }

        log.info("REST: GET domains for storeId: {}", storeId);
        try {
            List<DomainDto> result = storeService.getStoreDomain(storeId);
            return ApiResponse.success(result != null ? result : Collections.emptyList());
        } catch (Exception e) {
            log.error("Error fetching domains for store {}: {}", storeId, e.getMessage());
            return ApiResponse.error("Failed to fetch store domains: " + e.getMessage());
        }
    }

    /**
     * Search free / available domains by name.
     * Accessible via GET /api/v1/domains/free or /api/v1/domains/check.
     */
    @GetMapping({"/domains/free", "/domains/check"})
    public ApiResponse<List<DomainDto>> findFreeDomains(@RequestParam(value = "name", required = false) String name) {
        log.info("REST: GET find free domains for name: {}", name);
        try {
            if (name == null || name.trim().isEmpty()) {
                return ApiResponse.success(Collections.emptyList());
            }
            List<DomainDto> result = storeService.findFreeDomains(name);
            return ApiResponse.success(result != null ? result : Collections.emptyList());
        } catch (Exception e) {
            log.error("Error finding free domains for name {}: {}", name, e.getMessage());
            return ApiResponse.error("Failed to check domain availability: " + e.getMessage());
        }
    }

    /**
     * Helper to resolve tenantId from TenantContext or fallback to JWT token.
     */
    private UUID resolveTenantId(HttpServletRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }

        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            tenantId = jwtUtils.getTenantIdFromToken(jwt);
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }
        }
        return tenantId;
    }

    /**
     * Ensure TenantContext is populated for tenant-scoped operations.
     */
    private void ensureTenantContext(HttpServletRequest request) {
        if (TenantContext.getTenantId() == null) {
            resolveTenantId(request);
        }
    }
}
