package com.aiecomm.camp.modules.store.controller;

import com.aiecomm.camp.common.dto.ApiResponse;
import com.aiecomm.camp.modules.store.dto.StoreDto;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.serviceimpl.StoreServiceImpl;
import com.aiecomm.camp.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
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
}
