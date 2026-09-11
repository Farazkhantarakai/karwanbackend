package com.aiecomm.camp.modules.subscription.service;

import com.aiecomm.camp.modules.subscription.entity.StoreSubscriptionPlan;
import com.aiecomm.camp.modules.subscription.entity.SubscriptionPlan;
import com.aiecomm.camp.modules.subscription.exceptions.PlanLimitExceededException;
import com.aiecomm.camp.modules.subscription.repository.StoreSubscriptionPlanRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLimitService {

    private final StoreSubscriptionPlanRepository storeSubscriptionPlanRepository;

    // 1 GB default free storage quota
    public static final long DEFAULT_FREE_STORAGE_BYTES = 1L * 1024 * 1024 * 1024; // 1 GB (1,073,741,824 bytes)

    @Data
    @Builder
    public static class PlanCapabilities {
        private int maxProductImages;
        private long maxImageSizeBytes;
        private long maxVideoSizeBytes;
        private long maxStorageBytes;
        private String planName;
    }

    public PlanCapabilities getCapabilitiesForTenant(UUID tenantId) {
        if (tenantId != null) {
            try {
                Optional<StoreSubscriptionPlan> activeSub = storeSubscriptionPlanRepository.findFirstByTenant_TenantId(tenantId);
                if (activeSub.isPresent() && activeSub.get().getSubscriptionPlan() != null) {
                    SubscriptionPlan plan = activeSub.get().getSubscriptionPlan();
                    String planName = plan.getSubscriptionName() != null ? plan.getSubscriptionName().toLowerCase() : "free";

                    if (planName.contains("pro") || planName.contains("enterprise") || planName.contains("premium")) {
                        return PlanCapabilities.builder()
                                .maxProductImages(25)
                                .maxImageSizeBytes(25L * 1024 * 1024) // 25 MB
                                .maxVideoSizeBytes(500L * 1024 * 1024) // 500 MB
                                .maxStorageBytes(50L * 1024 * 1024 * 1024) // 50 GB
                                .planName(plan.getSubscriptionName())
                                .build();
                    } else if (planName.contains("basic") || planName.contains("starter")) {
                        return PlanCapabilities.builder()
                                .maxProductImages(15)
                                .maxImageSizeBytes(15L * 1024 * 1024) // 15 MB
                                .maxVideoSizeBytes(250L * 1024 * 1024) // 250 MB
                                .maxStorageBytes(10L * 1024 * 1024 * 1024) // 10 GB
                                .planName(plan.getSubscriptionName())
                                .build();
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not query subscription plan for tenant {}: {}", tenantId, ex.getMessage());
            }
        }

        // Default Free Plan Capabilities with 1 GB storage
        return PlanCapabilities.builder()
                .maxProductImages(10)
                .maxImageSizeBytes(10L * 1024 * 1024) // 10 MB per file
                .maxVideoSizeBytes(100L * 1024 * 1024) // 100 MB per file
                .maxStorageBytes(DEFAULT_FREE_STORAGE_BYTES) // 1 GB free total storage
                .planName("Free")
                .build();
    }

    public void validateFileSize(UUID tenantId, String contentType, Long sizeBytes) {
        if (sizeBytes == null || sizeBytes <= 0) return;
        PlanCapabilities capabilities = getCapabilitiesForTenant(tenantId);

        if (contentType != null && contentType.startsWith("video/")) {
            if (sizeBytes > capabilities.getMaxVideoSizeBytes()) {
                throw new PlanLimitExceededException(
                        "PRODUCT_VIDEO_SIZE_LIMIT_EXCEEDED",
                        String.format("This video exceeds your %s plan's limit of %d MB. Upgrade your plan to upload larger videos.",
                                capabilities.getPlanName(), capabilities.getMaxVideoSizeBytes() / (1024 * 1024)),
                        true
                );
            }
        } else {
            if (sizeBytes > capabilities.getMaxImageSizeBytes()) {
                throw new PlanLimitExceededException(
                        "PRODUCT_IMAGE_SIZE_LIMIT_EXCEEDED",
                        String.format("This image exceeds your %s plan's limit of %d MB.",
                                capabilities.getPlanName(), capabilities.getMaxImageSizeBytes() / (1024 * 1024)),
                        true
                );
            }
        }
    }

    public void validateStorageQuota(UUID tenantId, Long incomingSizeBytes, Long currentUsedBytes) {
        if (incomingSizeBytes == null || incomingSizeBytes <= 0) return;
        PlanCapabilities capabilities = getCapabilitiesForTenant(tenantId);
        long totalAfterUpload = (currentUsedBytes != null ? currentUsedBytes : 0L) + incomingSizeBytes;

        if (totalAfterUpload > capabilities.getMaxStorageBytes()) {
            double limitGb = capabilities.getMaxStorageBytes() / (1024.0 * 1024.0 * 1024.0);
            throw new PlanLimitExceededException(
                    "STORAGE_QUOTA_EXCEEDED",
                    String.format("Storage limit exceeded. Your %s plan includes %.1f GB free storage. Upgrade your plan for more space.",
                            capabilities.getPlanName(), limitGb),
                    true
            );
        }
    }

    public void validateProductImageCount(UUID tenantId, int count) {
        PlanCapabilities capabilities = getCapabilitiesForTenant(tenantId);
        if (count > capabilities.getMaxProductImages()) {
            throw new PlanLimitExceededException(
                    "PRODUCT_IMAGE_LIMIT_EXCEEDED",
                    String.format("Your current %s plan allows up to %d product images. Upgrade your plan to add more images.",
                            capabilities.getPlanName(), capabilities.getMaxProductImages()),
                    true
            );
        }
    }
}
