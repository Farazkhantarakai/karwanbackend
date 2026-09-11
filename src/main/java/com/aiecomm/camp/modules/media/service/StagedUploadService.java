package com.aiecomm.camp.modules.media.service;

import com.aiecomm.camp.core.TenantContext;
import com.aiecomm.camp.modules.media.dto.StagedUploadRequest;
import com.aiecomm.camp.modules.media.dto.StagedUploadResponse;
import com.aiecomm.camp.modules.media.repository.MediaAssetRepository;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.subscription.service.SubscriptionLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StagedUploadService {

    private final S3Presigner s3Presigner;
    private final StoreRepository storeRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final SubscriptionLimitService subscriptionLimitService;

    @Value("${cloudflare.r2.bucket:karwan-media}")
    private String bucket;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    public StagedUploadResponse createStagedUpload(Long storeId, StagedUploadRequest request) {
        // 1. Resolve & Validate Authenticated Tenant
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        // 2. Validate Store belongs to the Authenticated Tenant
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        if (store.getTenant() == null || !tenantId.equals(store.getTenant().getTenantId())) {
            throw new AccessDeniedException("Store does not belong to the authenticated tenant");
        }

        // 3. Strict MIME Type Allow-List (No SVG, No HTML, No executables)
        String rawContentType = request.getContentType() != null ? request.getContentType().toLowerCase().trim() : "";
        if (!ALLOWED_IMAGE_TYPES.contains(rawContentType)) {
            throw new IllegalArgumentException("Unsupported image format: '" + request.getContentType()
                    + "'. Allowed formats are JPEG, PNG, and WebP.");
        }

        // 4. Validate Individual File Size Limit (e.g. 10 MB on Free plan)
        subscriptionLimitService.validateFileSize(tenantId, rawContentType, request.getSize());

        // 5. Validate Total Storage Quota (1 GB Free Quota check)
        Long currentUsageBytes = mediaAssetRepository.sumSizeByTenantId(tenantId);
        subscriptionLimitService.validateStorageQuota(tenantId, request.getSize(), currentUsageBytes);

        // 6. Construct Scoped Staged Key: staged/tenants/{tenantId}/stores/{storeId}/{uuid}.{ext}
        String extension = determineExtension(request.getFileName(), rawContentType);
        String uuid = UUID.randomUUID().toString();
        String objectKey = String.format("staged/tenants/%s/stores/%s/%s%s",
                tenantId, storeId, uuid, extension);

        // 7. Generate Presigned PUT URL cryptographically bound to that exact key (SigV4)
        Duration expiry = Duration.ofMinutes(15);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(rawContentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presigned.url().toString();

        log.info("Generated staged upload target for tenant: {}, store: {}, objectKey: {}", tenantId, storeId, objectKey);

        // Zero database writes! Completely stateless.
        return StagedUploadResponse.builder()
                .uploadUrl(uploadUrl)
                .objectKey(objectKey)
                .expiresAt(Instant.now().plus(expiry))
                .build();
    }

    private String determineExtension(String fileName, String contentType) {
        if (fileName != null && fileName.contains(".")) {
            String sub = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
            if (sub.length() <= 6) return sub;
        }
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
