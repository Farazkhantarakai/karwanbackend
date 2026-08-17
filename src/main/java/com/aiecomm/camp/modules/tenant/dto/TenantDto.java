package com.aiecomm.camp.modules.tenant.dto;

import com.aiecomm.camp.modules.tenant.TenatEnums.OnboardingStatus;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantDto {

    private UUID tenantId;
    private OnboardingStatus onBoardingStatus;
    private Instant createdOn;
    private Instant updatedOn;

    public static TenantDto fromEntity(Tenant tenant) {
        if (tenant == null) return null;
        return TenantDto.builder()
                .tenantId(tenant.getTenantId())
                .onBoardingStatus(tenant.getOnBoardingStatus())
                .createdOn(tenant.getCreatedOn())
                .updatedOn(tenant.getUpdatedOn())
                .build();
    }
}
