package com.aiecomm.camp.modules.subscription.dto;

import com.aiecomm.camp.modules.subscription.entity.TenantSubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSubscriptionPlanDto {

    private Long subId;
    private UUID tenantId;
    private Long subscriptionId;

    public static TenantSubscriptionPlanDto fromEntity(TenantSubscriptionPlan entity) {
        if (entity == null) return null;
        return TenantSubscriptionPlanDto.builder()
                .subId(entity.getSubId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getTenantId() : null)
                .subscriptionId(entity.getSubscriptionPlan() != null ? entity.getSubscriptionPlan().getSubscriptionId() : null)
                .build();
    }
}
