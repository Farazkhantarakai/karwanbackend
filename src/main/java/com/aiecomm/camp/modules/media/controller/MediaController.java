package com.aiecomm.camp.modules.media.controller;

import com.aiecomm.camp.common.dto.ApiResponse;
import com.aiecomm.camp.modules.media.dto.*;
import com.aiecomm.camp.modules.media.exception.AssetReferencedException;
import com.aiecomm.camp.modules.media.service.MediaAssetService;
import com.aiecomm.camp.modules.media.service.R2MediaService;
import com.aiecomm.camp.modules.media.service.R2PresignedUrlService;
import com.aiecomm.camp.modules.media.service.StagedUploadService;
import com.aiecomm.camp.modules.subscription.exceptions.PlanLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/media", "/api/media"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class MediaController {

    private final StagedUploadService stagedUploadService;
    private final MediaAssetService mediaAssetService;
    private final R2MediaService r2MediaService;
    private final R2PresignedUrlService r2PresignedUrlService;

    /**
     * Step 1: Staged Upload Target (Stateless Presigned PUT URL)
     * POST /api/v1/media/staged-upload (and aliased to /upload-intent)
     */
    @PostMapping({"/staged-upload", "/upload-intent"})
    public ResponseEntity<ApiResponse<StagedUploadResponse>> createStagedUpload(
            @RequestParam(value = "storeId", required = false) Long paramStoreId,
            @RequestBody StagedUploadRequest request) {

        Long storeId = request.getStoreId() != null ? request.getStoreId() : paramStoreId;
        request.setStoreId(storeId);

        log.info("REST: Staged upload request for store: {}, fileName: {}, size: {}",
                storeId, request.getFileName(), request.getSize());

        try {
            StagedUploadResponse response = stagedUploadService.createStagedUpload(storeId, request);
            return ResponseEntity.ok(ApiResponse.success("Staged upload target created successfully", response));
        } catch (PlanLimitExceededException e) {
            log.warn("Subscription plan limit exceeded: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponse.error(e.getMessage(), e.getCode(), e.isUpgradeRequired()));
        } catch (AccessDeniedException e) {
            log.warn("Access denied creating staged upload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage(), "ACCESS_DENIED", false));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating staged upload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR", false));
        } catch (Exception e) {
            log.error("Unexpected error creating staged upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Unable to generate upload target: " + e.getMessage()));
        }
    }

    /**
     * Step 3: Register Media Asset (Mirrors Shopify fileCreate)
     * POST /api/v1/media/assets
     */
    @PostMapping("/assets")
    public ResponseEntity<ApiResponse<MediaAssetDto>> registerAsset(
            @RequestParam(value = "storeId", required = false) Long paramStoreId,
            @RequestBody RegisterAssetRequest request) {

        Long storeId = request.getStoreId() != null ? request.getStoreId() : paramStoreId;
        request.setStoreId(storeId);

        log.info("REST: POST /media/assets for store: {}, objectKey: {}", storeId, request.getObjectKey());
        try {
            MediaAssetDto response = mediaAssetService.registerAsset(storeId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Media asset registered and queued for promotion", response));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage(), "ACCESS_DENIED", false));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "VALIDATION_ERROR", false));
        } catch (Exception e) {
            log.error("Failed to register media asset", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to register media asset: " + e.getMessage()));
        }
    }

    /**
     * Polling endpoint: check asset promotion status
     * GET /api/v1/media/assets/{id}
     */
    @GetMapping("/assets/{id}")
    public ResponseEntity<ApiResponse<MediaAssetDto>> getAssetStatus(@PathVariable Long id) {
        try {
            MediaAssetDto asset = mediaAssetService.getAssetStatus(id);
            return ResponseEntity.ok(ApiResponse.success(asset));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Asset not found", "NOT_FOUND", false));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage(), "ACCESS_DENIED", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error retrieving asset: " + e.getMessage()));
        }
    }

    /**
     * Browse store media library
     * GET /api/v1/media/assets?storeId={storeId}
     */
    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<Page<MediaAssetDto>>> getStoreAssets(
            @RequestParam("storeId") Long storeId,
            @RequestParam(value = "search", required = false) String search,
            @PageableDefault(size = 24) Pageable pageable) {
        try {
            Page<MediaAssetDto> page = mediaAssetService.getStoreAssets(storeId, search, pageable);
            return ResponseEntity.ok(ApiResponse.success(page));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage(), "ACCESS_DENIED", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error browsing store assets: " + e.getMessage()));
        }
    }

    /**
     * Reference-checked deletion: rejects with 409 Conflict if in use
     * DELETE /api/v1/media/assets/{id}
     */
    @DeleteMapping("/assets/{id}")
    public ResponseEntity<ApiResponse<?>> deleteAsset(@PathVariable Long id) {
        try {
            mediaAssetService.deleteAsset(id);
            return ResponseEntity.noContent().build();
        } catch (AssetReferencedException e) {
            log.warn("Cannot delete asset {}: still referenced by {} consumer(s)", id, e.getReferences().size());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            "Cannot delete asset: it is in use by " + e.getReferences().size() + " consumer(s)",
                            "ASSET_REFERENCED",
                            false
                    ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Asset not found", "NOT_FOUND", false));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied: " + e.getMessage(), "ACCESS_DENIED", false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete asset: " + e.getMessage()));
        }
    }

    /**
     * Storage Quota endpoint: returns 1 GB free quota usage
     * GET /api/v1/media/storage-usage?storeId={storeId}
     */
    @GetMapping("/storage-usage")
    public ResponseEntity<ApiResponse<StorageUsageDto>> getStorageUsage(@RequestParam("storeId") Long storeId) {
        try {
            StorageUsageDto usage = mediaAssetService.getStorageUsage(storeId);
            return ResponseEntity.ok(ApiResponse.success(usage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Could not determine storage usage: " + e.getMessage()));
        }
    }

    /**
     * Legacy backward-compatibility endpoints
     */
    @PostMapping("/{mediaId}/complete")
    public ApiResponse<MediaCompleteResponse> completeUpload(@PathVariable UUID mediaId) {
        try {
            MediaCompleteResponse response = r2MediaService.completeUpload(mediaId);
            return ApiResponse.success("Upload verified and completed", response);
        } catch (Exception e) {
            return ApiResponse.error("Upload verification failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload-url")
    public ApiResponse<PresignedUrlResponse> generateUploadUrl(@RequestBody PresignedUrlRequest request) {
        try {
            PresignedUrlResponse response = r2PresignedUrlService.generatePresignedUploadUrl(request);
            return ApiResponse.success("Presigned upload URL generated successfully", response);
        } catch (Exception e) {
            return ApiResponse.error("Failed to generate presigned URL: " + e.getMessage());
        }
    }
}
