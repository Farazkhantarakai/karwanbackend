package com.aiecomm.camp.modules.media.service;

import com.aiecomm.camp.core.TenantContext;
import com.aiecomm.camp.modules.media.dto.MediaCompleteResponse;
import com.aiecomm.camp.modules.media.dto.UploadIntentRequest;
import com.aiecomm.camp.modules.media.dto.UploadIntentResponse;
import com.aiecomm.camp.modules.media.entity.Media;
import com.aiecomm.camp.modules.media.entity.MediaStatus;
import com.aiecomm.camp.modules.media.repository.MediaRepository;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.subscription.service.SubscriptionLimitService;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class R2MediaService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MediaRepository mediaRepository;
    private final StoreRepository storeRepository;
    private final SubscriptionLimitService subscriptionLimitService;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.public-domain:}")
    private String publicDomain;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm"
    );

    @Transactional
    public UploadIntentResponse createUploadIntent(UploadIntentRequest request) {
        // 1. Resolve & Enforce Tenant from Security Context
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        if (request.getStoreId() == null) {
            throw new IllegalArgumentException("storeId is required");
        }

        // 2. Validate Store belongs to the Authenticated Tenant
        Optional<Store> storeOpt = storeRepository.findById(request.getStoreId());
        if (storeOpt.isEmpty()) {
            throw new IllegalArgumentException("Store not found: " + request.getStoreId());
        }
        Store store = storeOpt.get();
        if (store.getTenant() == null || !tenantId.equals(store.getTenant().getTenantId())) {
            throw new AccessDeniedException("Store does not belong to the authenticated tenant");
        }

        // 3. Strict MIME Type Validation (No SVG, No HTML, No executables)
        String rawContentType = request.getContentType() != null ? request.getContentType().toLowerCase().trim() : "";
        boolean isImage = ALLOWED_IMAGE_TYPES.contains(rawContentType);
        boolean isVideo = ALLOWED_VIDEO_TYPES.contains(rawContentType);

        if (!isImage && !isVideo) {
            throw new IllegalArgumentException("Unsupported file type: " + request.getContentType()
                    + ". Allowed types are: JPEG, PNG, WebP, MP4.");
        }

        // 4. File Size & Subscription Plan Validation
        subscriptionLimitService.validateFileSize(tenantId, rawContentType, request.getSize());

        // 5. Generate unique MediaId and multi-tenant Object Key
        UUID mediaId = UUID.randomUUID();
        String extension = determineExtension(request.getFileName(), rawContentType);
        String objectKey = String.format("tenants/%s/stores/%s/uploads/%s%s",
                tenantId, store.getStoreId(), mediaId, extension);

        // 6. Create Media record in status PENDING
        Media media = Media.builder()
                .id(mediaId)
                .tenant(store.getTenant())
                .store(store)
                .objectKey(objectKey)
                .originalFilename(sanitizeFilename(request.getFileName()))
                .contentType(rawContentType)
                .size(request.getSize())
                .status(MediaStatus.PENDING)
                .build();

        mediaRepository.save(media);
        log.info("Created PENDING media record: {} for objectKey: {}", mediaId, objectKey);

        // 7. Generate Presigned PUT URL (15 min for image, 30 min for video)
        Duration duration = isVideo ? Duration.ofMinutes(30) : Duration.ofMinutes(15);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(rawContentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(duration)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        String publicUrl = buildPublicUrl(objectKey);

        return UploadIntentResponse.builder()
                .mediaId(mediaId)
                .uploadUrl(presigned.url().toString())
                .url(publicUrl)
                .objectKey(objectKey)
                .expiresIn(duration.getSeconds())
                .build();
    }

    @Transactional
    public MediaCompleteResponse completeUpload(UUID mediaId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        Media media = mediaRepository.findByIdAndTenant_TenantId(mediaId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Media not found or unauthorized: " + mediaId));

        if (media.getStatus() == MediaStatus.ATTACHED || media.getStatus() == MediaStatus.UPLOADED) {
            return buildCompleteResponse(media);
        }

        // 1. Verify object actually exists in Cloudflare R2
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(media.getObjectKey())
                    .build());
        } catch (S3Exception ex) {
            log.error("Upload verification failed: object {} not found in bucket {}", media.getObjectKey(), bucket, ex);
            media.setStatus(MediaStatus.FAILED);
            mediaRepository.save(media);
            throw new IllegalStateException("Uploaded file was not found in storage: " + ex.getMessage());
        }

        // 2. Validate Size
        long actualSize = head.contentLength() != null ? head.contentLength() : 0L;
        media.setSize(actualSize);

        // 3. Inspect Image Dimensions if it's an image
        if (media.getContentType().startsWith("image/")) {
            inspectImageDimensions(media);
        }

        media.setStatus(MediaStatus.UPLOADED);
        media.setUploadedAt(Instant.now());
        Media saved = mediaRepository.save(media);
        log.info("Media {} successfully verified and marked UPLOADED (size: {} bytes, dims: {}x{})",
                mediaId, actualSize, saved.getWidth(), saved.getHeight());

        return buildCompleteResponse(saved);
    }

    private void inspectImageDimensions(Media media) {
        try (ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(media.getObjectKey())
                .range("bytes=0-1048576") // read first 1MB for header metadata
                .build())) {

            BufferedImage img = ImageIO.read(s3Stream);
            if (img != null) {
                media.setWidth(img.getWidth());
                media.setHeight(img.getHeight());
                log.debug("Extracted dimensions for media {}: {}x{}", media.getId(), img.getWidth(), img.getHeight());
            }
        } catch (Exception ex) {
            log.warn("Could not read image dimensions for {}: {}", media.getObjectKey(), ex.getMessage());
        }
    }

    public String buildPublicUrl(String objectKey) {
        if (publicDomain != null && !publicDomain.isBlank()) {
            String trimmed = publicDomain.endsWith("/") ? publicDomain.substring(0, publicDomain.length() - 1) : publicDomain;
            return trimmed + "/" + objectKey;
        }
        String trimmedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return trimmedEndpoint + "/" + bucket + "/" + objectKey;
    }

    private MediaCompleteResponse buildCompleteResponse(Media media) {
        return MediaCompleteResponse.builder()
                .mediaId(media.getId())
                .status(media.getStatus())
                .url(buildPublicUrl(media.getObjectKey()))
                .size(media.getSize())
                .width(media.getWidth())
                .height(media.getHeight())
                .contentType(media.getContentType())
                .originalFilename(media.getOriginalFilename())
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
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            default -> "";
        };
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "upload";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Requirement #31: Abandoned Upload Cleanup
     * Runs periodically to delete PENDING or unattached UPLOADED media older than 24 hours.
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    public void cleanupAbandonedMedia() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        log.info("Running abandoned media cleanup for records older than {}", cutoff);

        List<Media> abandoned = mediaRepository.findAbandonedUnattachedMedia(
                List.of(MediaStatus.PENDING, MediaStatus.UPLOADED),
                cutoff
        );

        for (Media m : abandoned) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(m.getObjectKey())
                        .build());
                m.setStatus(MediaStatus.DELETED);
                mediaRepository.save(m);
                log.info("Cleaned up abandoned media {} (objectKey: {})", m.getId(), m.getObjectKey());
            } catch (Exception ex) {
                log.warn("Failed to delete abandoned R2 object {}: {}", m.getObjectKey(), ex.getMessage());
            }
        }
    }
}
