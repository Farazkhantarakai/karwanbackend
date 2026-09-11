package com.aiecomm.camp.modules.media.service;

import com.aiecomm.camp.core.TenantContext;
import com.aiecomm.camp.modules.media.dto.ConsumerRefDto;
import com.aiecomm.camp.modules.media.dto.MediaAssetDto;
import com.aiecomm.camp.modules.media.dto.RegisterAssetRequest;
import com.aiecomm.camp.modules.media.dto.StorageUsageDto;
import com.aiecomm.camp.modules.media.entity.AssetStatus;
import com.aiecomm.camp.modules.media.entity.MediaAsset;
import com.aiecomm.camp.modules.media.entity.MediaReference;
import com.aiecomm.camp.modules.media.exception.AssetReferencedException;
import com.aiecomm.camp.modules.media.repository.MediaAssetRepository;
import com.aiecomm.camp.modules.media.repository.MediaReferenceRepository;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.subscription.service.SubscriptionLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaAssetService {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaReferenceRepository mediaReferenceRepository;
    private final StoreRepository storeRepository;
    private final SubscriptionLimitService subscriptionLimitService;
    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket:karwan-media}")
    private String bucket;

    @Value("${cloudflare.r2.endpoint:}")
    private String endpoint;

    @Value("${cloudflare.r2.public-domain:}")
    private String publicDomain;

    /**
     * Step 3: Register Asset (Mirrors Shopify fileCreate)
     * Creates media_assets row in status UPLOADED and enqueues async promotion.
     */
    @Transactional
    public MediaAssetDto registerAsset(Long storeId, RegisterAssetRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        // 1. Validate Store Ownership
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        if (store.getTenant() == null || !tenantId.equals(store.getTenant().getTenantId())) {
            throw new AccessDeniedException("Store does not belong to the authenticated tenant");
        }

        // 2. Path Ownership Validation (Crucial Security Check)
        String expectedPrefix = String.format("staged/tenants/%s/stores/%s/", tenantId, storeId);
        if (request.getObjectKey() == null || !request.getObjectKey().startsWith(expectedPrefix)) {
            throw new AccessDeniedException("Object key does not belong to this tenant/store scope");
        }

        // 3. Idempotency Check
        Optional<MediaAsset> existing = mediaAssetRepository.findByObjectKey(request.getObjectKey());
        if (existing.isPresent()) {
            return MediaAssetDto.fromEntity(existing.get());
        }

        // 4. Insert media_assets record in status UPLOADED
        MediaAsset asset = MediaAsset.builder()
                .tenant(store.getTenant())
                .store(store)
                .objectKey(request.getObjectKey())
                .originalFilename(sanitizeFilename(request.getOriginalFilename()))
                .contentType(request.getContentType() != null ? request.getContentType().toLowerCase().trim() : "image/jpeg")
                .status(AssetStatus.UPLOADED)
                .build();

        MediaAsset saved = mediaAssetRepository.save(asset);
        log.info("Registered media asset ID: {} for objectKey: {}", saved.getId(), saved.getObjectKey());

        // 5. Trigger Decoupled Promotion Worker (Async)
        promoteAssetAsync(saved.getId(), tenantId);

        return MediaAssetDto.fromEntity(saved);
    }

    /**
     * Async Promotion Worker:
     * 1. Confirms file landed in R2 via headObject
     * 2. Re-encodes image via ImageIO to strip metadata & polyglot scripts
     * 3. Uploads to permanent products/ prefix
     * 4. Deletes staged/ object (single-use promotion / replay prevention)
     * 5. Sets status = READY
     */
    @Async
    public void promoteAssetAsync(Long assetId, UUID tenantId) {
        try {
            promoteAssetTask(assetId, tenantId);
        } catch (Exception ex) {
            log.error("Failed to promote asset ID: {}", assetId, ex);
        }
    }

    @Transactional
    public void promoteAssetTask(Long assetId, UUID tenantId) {
        Optional<MediaAsset> opt = mediaAssetRepository.findById(assetId);
        if (opt.isEmpty()) {
            log.error("Asset not found for promotion: {}", assetId);
            return;
        }

        MediaAsset asset = opt.get();
        if (asset.getStatus() == AssetStatus.READY) {
            return; // Already promoted
        }

        asset.setStatus(AssetStatus.PROCESSING);
        mediaAssetRepository.save(asset);

        String stagedKey = asset.getObjectKey();

        try {
            // 1. Verify object exists in R2
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(stagedKey)
                    .build());

            if (head == null) {
                markFailed(asset, "Object not found in storage after upload");
                return;
            }

            // 2. Download bytes and re-encode through ImageIO (polyglot & metadata neutralization)
            byte[] reencodedBytes;
            int width;
            int height;
            String formatName = getFormatName(asset.getContentType());

            try (ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(stagedKey)
                    .build())) {

                BufferedImage img = ImageIO.read(s3Stream);
                if (img == null) {
                    markFailed(asset, "Invalid image raster. File could not be decoded as an authentic image.");
                    return;
                }

                width = img.getWidth();
                height = img.getHeight();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                boolean written = ImageIO.write(img, formatName, baos);
                if (!written) {
                    // Fallback to jpeg
                    baos.reset();
                    ImageIO.write(img, "jpg", baos);
                }
                reencodedBytes = baos.toByteArray();
            }

            // 3. Upload re-encoded bytes to Permanent Key: products/tenants/{tid}/stores/{sid}/{uuid}.{ext}
            String ext = "." + (formatName.equals("png") ? "png" : formatName.equals("webp") ? "webp" : "jpg");
            String permanentKey = String.format("products/tenants/%s/stores/%s/%s%s",
                    asset.getTenant().getTenantId(),
                    asset.getStore().getStoreId(),
                    UUID.randomUUID(),
                    ext);

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(permanentKey)
                            .contentType(asset.getContentType())
                            .build(),
                    RequestBody.fromBytes(reencodedBytes));

            // 4. Delete staged object from R2 (Single-Use Replay Prevention)
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(stagedKey)
                        .build());
            } catch (Exception delEx) {
                log.warn("Could not delete staged object {}: {}", stagedKey, delEx.getMessage());
            }

            // 5. Update media_assets as READY
            String publicUrl = buildPublicUrl(permanentKey);
            asset.setObjectKey(permanentKey);
            asset.setPublicUrl(publicUrl);
            asset.setSize((long) reencodedBytes.length);
            asset.setWidth(width);
            asset.setHeight(height);
            asset.setStatus(AssetStatus.READY);
            asset.setReadyAt(Instant.now());
            asset.setErrorMessage(null);

            mediaAssetRepository.save(asset);
            log.info("Asset {} successfully promoted to READY with permanentKey: {} (dims: {}x{})",
                    assetId, permanentKey, width, height);

        } catch (Exception ex) {
            log.error("Error during promotion of asset {}: {}", assetId, ex.getMessage(), ex);
            markFailed(asset, "Promotion error: " + ex.getMessage());
        }
    }

    private void markFailed(MediaAsset asset, String error) {
        asset.setStatus(AssetStatus.FAILED);
        asset.setErrorMessage(error != null && error.length() > 490 ? error.substring(0, 490) : error);
        mediaAssetRepository.save(asset);
    }

    /**
     * Polling endpoint: returns asset status & metadata for frontend readiness tracking
     */
    @Transactional(readOnly = true)
    public MediaAssetDto getAssetStatus(Long assetId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        MediaAsset asset = mediaAssetRepository.findByIdAndTenant_TenantId(assetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Media asset not found: " + assetId));

        return MediaAssetDto.fromEntity(asset);
    }

    /**
     * Gallery endpoint: returns paginated media assets for a store
     */
    @Transactional(readOnly = true)
    public Page<MediaAssetDto> getStoreAssets(Long storeId, String search, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        Page<MediaAsset> page;
        if (search != null && !search.trim().isEmpty()) {
            page = mediaAssetRepository.findByStore_StoreIdAndTenant_TenantIdAndOriginalFilenameContainingIgnoreCase(
                    storeId, tenantId, search.trim(), pageable);
        } else {
            page = mediaAssetRepository.findByStore_StoreIdAndTenant_TenantIdAndStatus(
                    storeId, tenantId, AssetStatus.READY, pageable);
        }

        return page.map(MediaAssetDto::fromEntity);
    }

    /**
     * Reference-checked deletion: rejects with 409 Conflict if still referenced
     */
    @Transactional
    public void deleteAsset(Long assetId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        MediaAsset asset = mediaAssetRepository.findByIdAndTenant_TenantId(assetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Media asset not found: " + assetId));

        // Reference Check
        List<MediaReference> references = mediaReferenceRepository.findByMediaAsset_Id(assetId);
        if (!references.isEmpty()) {
            List<ConsumerRefDto> consumers = references.stream()
                    .map(r -> new ConsumerRefDto(r.getReferenceableType(), r.getReferenceableId()))
                    .collect(Collectors.toList());
            throw new AssetReferencedException(assetId, consumers);
        }

        // Soft delete status
        asset.setStatus(AssetStatus.DELETING);
        mediaAssetRepository.save(asset);

        // Delete from R2
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(asset.getObjectKey())
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to delete R2 object {}: {}", asset.getObjectKey(), ex.getMessage());
        }

        asset.setStatus(AssetStatus.DELETED);
        mediaAssetRepository.save(asset);
        log.info("Media asset {} successfully deleted", assetId);
    }

    /**
     * Storage usage quota tracker (1 GB free quota tracking)
     */
    @Transactional(readOnly = true)
    public StorageUsageDto getStorageUsage(Long storeId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        var capabilities = subscriptionLimitService.getCapabilitiesForTenant(tenantId);
        Long usedBytes = mediaAssetRepository.sumSizeByTenantId(tenantId);
        long limitBytes = capabilities.getMaxStorageBytes();

        double usedPercent = (limitBytes > 0) ? Math.min(100.0, ((double) usedBytes / limitBytes) * 100.0) : 0.0;

        return StorageUsageDto.builder()
                .usedBytes(usedBytes)
                .limitBytes(limitBytes)
                .usedPercentage(Math.round(usedPercent * 10.0) / 10.0)
                .formattedUsed(formatBytes(usedBytes))
                .formattedLimit(formatBytes(limitBytes))
                .build();
    }

    /**
     * Scheduled job: cleans up orphaned/stuck uploads older than 48 hours
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    public void cleanupOrphanedMedia() {
        Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);
        log.info("Running orphaned media cleanup for records older than {}", cutoff);

        List<MediaAsset> stale = mediaAssetRepository.findByStatusInAndCreatedAtBefore(
                List.of(AssetStatus.UPLOADED, AssetStatus.PROCESSING, AssetStatus.FAILED),
                cutoff
        );

        for (MediaAsset asset : stale) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(asset.getObjectKey())
                        .build());
                asset.setStatus(AssetStatus.DELETED);
                mediaAssetRepository.save(asset);
                log.info("Reclaimed stale asset {} (key: {})", asset.getId(), asset.getObjectKey());
            } catch (Exception ex) {
                log.warn("Failed to delete stale R2 object {}: {}", asset.getObjectKey(), ex.getMessage());
            }
        }
    }

    public String buildPublicUrl(String objectKey) {
        if (publicDomain != null && !publicDomain.isBlank()) {
            String trimmed = publicDomain.endsWith("/") ? publicDomain.substring(0, publicDomain.length() - 1) : publicDomain;
            return trimmed + "/" + objectKey;
        }
        String trimmedEndpoint = endpoint != null && endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return (trimmedEndpoint != null && !trimmedEndpoint.isBlank() ? trimmedEndpoint + "/" + bucket : "") + "/" + objectKey;
    }

    private String getFormatName(String contentType) {
        if (contentType == null) return "jpg";
        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "image.jpg";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.1f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}
