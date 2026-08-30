package com.aiecomm.camp.modules.store.controller;

import com.aiecomm.camp.common.dto.ApiResponse;
import com.aiecomm.camp.modules.store.dto.DomainDto;
import com.aiecomm.camp.modules.store.dto.StoreDto;
import com.aiecomm.camp.modules.store.entity.Domain;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.serviceimpl.StoreServiceImpl;
import com.aiecomm.camp.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.aiecomm.camp.security.JwtAuthenticationFilter.parseJwt;

@RestController
@RequestMapping({"/api/v1", "/api"})
public class StoreController {

    @Autowired
    private StoreServiceImpl storeServiceImpl;

    @Autowired
    private JwtUtils jwtUtils;

    Logger logger= LoggerFactory.getLogger(StoreController.class);


    @GetMapping("/store")
    public ApiResponse<?> checkStoreForTheUser(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            UUID tenantId = jwtUtils.getTenantIdFromToken(jwt);
            if (tenantId != null) {
                List<Store> stores = storeServiceImpl.getStoreStatusForUser(tenantId);
                List<StoreDto> storeDtos = stores.stream()
                        .map(StoreDto::fromEntity)
                        .collect(Collectors.toList());
                return ApiResponse.success(storeDtos);
            }
        }
        return ApiResponse.error("Invalid token or tenant");
    }

    @PostMapping("/store")
    public ApiResponse<?> createStoreAgainstTheTenant(@RequestBody StoreDto storeDto, HttpServletRequest request) {
        UUID tenantId = storeDto.getTenantId();

        // Fallback: If tenantId is not in request body, extract directly from JWT token in request header
        if (tenantId == null) {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                tenantId = jwtUtils.getTenantIdFromToken(jwt);
            }
        }

        if (tenantId == null) {
            return ApiResponse.error("Tenant ID is required to create a store");
        }

        Store store = storeServiceImpl.createStoreAgainstTheTenant(
                tenantId,
                storeDto.getDomainname(),
                storeDto.getStorename()
        );

        if (store != null) {
            return ApiResponse.success(StoreDto.fromEntity(store));
        }

        return ApiResponse.error("Something went wrong while creating store");
    }





    @GetMapping("/domains")
    public ApiResponse<List<DomainDto>> getStoreDomains(@RequestParam Long storeId) {
        List<DomainDto> result = null;
        try {
            result = storeServiceImpl.getStoreDomain(storeId);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return ApiResponse.success(result);
    }

    @GetMapping({"/domains/free", "/domains/check", "/v1/domains/free", "/v1/domains/check"})
    public ApiResponse<List<DomainDto>> findFreeDomains(@RequestParam(value = "name", required = false) String name) {
        List<DomainDto> result = null;
        try {
            if (name == null || name.trim().isEmpty()) {
                return ApiResponse.success(java.util.Collections.emptyList());
            }
            result = storeServiceImpl.findFreeDomains(name);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error finding free domains for name {}: {}", name, e.getMessage());
            return ApiResponse.error("Failed to check domain availability: " + e.getMessage());
        }
        return ApiResponse.success(result != null ? result : java.util.Collections.emptyList());
    }
}
