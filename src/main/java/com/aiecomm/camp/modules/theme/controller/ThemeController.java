package com.aiecomm.camp.modules.theme.controller;

import com.aiecomm.camp.common.dto.ApiResponse;
import com.aiecomm.camp.modules.theme.dto.ThemeVersionDto;
import com.aiecomm.camp.modules.theme.dto.UpdateThemeDraftRequest;
import com.aiecomm.camp.modules.theme.service.ThemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1", "/api"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ThemeController {

    private final ThemeService themeService;

    @GetMapping("/stores/{storeId}/theme/draft")
    public ApiResponse<ThemeVersionDto> getDraftTheme(@PathVariable Long storeId) {
        log.info("REST: GET /stores/{}/theme/draft", storeId);
        try {
            return ApiResponse.success(themeService.getDraftTheme(storeId));
        } catch (AccessDeniedException e) {
            return ApiResponse.error(e.getMessage(), "ACCESS_DENIED", false);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching draft theme", e);
            return ApiResponse.error("Failed to fetch draft theme: " + e.getMessage());
        }
    }

    @PutMapping("/stores/{storeId}/theme/draft")
    public ApiResponse<ThemeVersionDto> updateDraftTheme(
            @PathVariable Long storeId,
            @RequestBody UpdateThemeDraftRequest request) {
        log.info("REST: PUT /stores/{}/theme/draft", storeId);
        try {
            return ApiResponse.success("Draft saved", themeService.updateDraftTheme(storeId, request.getConfiguration()));
        } catch (AccessDeniedException e) {
            return ApiResponse.error(e.getMessage(), "ACCESS_DENIED", false);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating draft theme", e);
            return ApiResponse.error("Failed to update draft theme: " + e.getMessage());
        }
    }

    @PostMapping("/stores/{storeId}/theme/publish")
    public ApiResponse<ThemeVersionDto> publishTheme(@PathVariable Long storeId) {
        log.info("REST: POST /stores/{}/theme/publish", storeId);
        try {
            return ApiResponse.success("Theme published", themeService.publishTheme(storeId));
        } catch (AccessDeniedException e) {
            return ApiResponse.error(e.getMessage(), "ACCESS_DENIED", false);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error publishing theme", e);
            return ApiResponse.error("Failed to publish theme: " + e.getMessage());
        }
    }

    @GetMapping("/stores/{storeId}/theme/published")
    public ApiResponse<ThemeVersionDto> getPublishedTheme(@PathVariable Long storeId) {
        log.info("REST: GET /stores/{}/theme/published", storeId);
        try {
            return ApiResponse.success(themeService.getPublishedTheme(storeId));
        } catch (AccessDeniedException e) {
            return ApiResponse.error(e.getMessage(), "ACCESS_DENIED", false);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching published theme", e);
            return ApiResponse.error("Failed to fetch published theme: " + e.getMessage());
        }
    }
}
