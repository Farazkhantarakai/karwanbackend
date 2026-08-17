package com.aiecomm.camp.modules.tenant.dto;

import com.aiecomm.camp.modules.tenant.entity.TenantUser;
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
public class TenantUserDto {

    private Long id;
    private Long userId;
    private UUID tenantId;
    private String role;
    private Instant createdOn;
    private Instant updatedOn;

    public static TenantUserDto fromEntity(TenantUser tenantUser) {
        if (tenantUser == null) return null;
        return TenantUserDto.builder()
                .id(tenantUser.getId())
                .userId(tenantUser.getUser() != null ? tenantUser.getUser().getId() : null)
                .tenantId(tenantUser.getTenant() != null ? tenantUser.getTenant().getTenantId() : null)
                .role(tenantUser.getRole())
                .createdOn(tenantUser.getCreatedOn())
                .updatedOn(tenantUser.getUpdatedOn())
                .build();
    }
}
