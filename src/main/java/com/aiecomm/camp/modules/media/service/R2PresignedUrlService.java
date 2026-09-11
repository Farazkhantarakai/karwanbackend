package com.aiecomm.camp.modules.media.service;

import com.aiecomm.camp.core.TenantContext;
import com.aiecomm.camp.modules.media.dto.PresignedUrlRequest;
import com.aiecomm.camp.modules.media.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class R2PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${cloudflare.r2.bucket}")
    private String bucket;

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.public-domain:}")
    private String publicDomain;

    public PresignedUrlResponse generatePresignedUploadUrl(PresignedUrlRequest request) {
        // 1. Resolve Tenant ID strictly from authenticated TenantContext (never from request body)
        UUID contextTenantId = TenantContext.getTenantId();
        String tenantId = (contextTenantId != null) ? contextTenantId.toString() : "global";

        // 2. Resolve Store ID and Product ID
        String storeId = (request.getStoreId() != null) ? String.valueOf(request.getStoreId()) : "default";
        String productId = (request.getProductId() != null && !request.getProductId().trim().isEmpty())
                ? request.getProductId().trim()
                : "temp";

        // 3. Resolve File ID and Extension
        String fileId = (request.getFileId() != null && !request.getFileId().trim().isEmpty())
                ? request.getFileId().trim()
                : UUID.randomUUID().toString();

        String extension = extractExtension(request.getFileName(), request.getContentType());

        // 4. Construct Object Key: tenants/{tenantId}/stores/{storeId}/products/{productId}/{fileId}.{ext}
        String objectKey = String.format(
                "tenants/%s/stores/%s/products/%s/%s%s",
                tenantId,
                storeId,
                productId,
                fileId,
                extension
        );

        String contentType = (request.getContentType() != null && !request.getContentType().isBlank())
                ? request.getContentType()
                : "application/octet-stream";

        log.info("Generating presigned PUT URL for bucket: {}, objectKey: {}, contentType: {}", bucket, objectKey, contentType);

        // 5. Build S3 Presign Request
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presigned.url().toString();

        // 6. Build Public URL
        String publicUrl;
        if (publicDomain != null && !publicDomain.isBlank()) {
            String trimmedDomain = publicDomain.endsWith("/") ? publicDomain.substring(0, publicDomain.length() - 1) : publicDomain;
            publicUrl = trimmedDomain + "/" + objectKey;
        } else {
            String trimmedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            publicUrl = trimmedEndpoint + "/" + bucket + "/" + objectKey;
        }

        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .publicUrl(publicUrl)
                .objectKey(objectKey)
                .fileId(fileId)
                .bucket(bucket)
                .build();
    }

    private String extractExtension(String fileName, String contentType) {
        if (fileName != null && fileName.contains(".")) {
            String sub = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
            if (sub.length() <= 8) {
                return sub;
            }
        }
        if (contentType != null) {
            return switch (contentType.toLowerCase()) {
                case "image/png" -> ".png";
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/webp" -> ".webp";
                case "image/gif" -> ".gif";
                case "image/svg+xml" -> ".svg";
                case "video/mp4" -> ".mp4";
                case "video/webm" -> ".webm";
                default -> "";
            };
        }
        return "";
    }
}
