package com.aiecomm.camp.modules.store.dto;

import com.aiecomm.camp.modules.store.entity.Store;
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
public class StoreDto {

    private Long storeId;
    private UUID tenantId;
    private String storename;
    private String domainname;
    private String status;
    private Instant trailStart;
    private Instant trailEnd;
    private String location;
    private String template;
    private Instant createdOn;
    private Instant updatedOn;

    public static StoreDto fromEntity(Store entity) {
        if (entity == null) return null;
        return StoreDto.builder()
                .storeId(entity.getStoreId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getTenantId() : null)
                .storename(entity.getStorename())
                .domainname(entity.getDomainname())
                .status(entity.getStatus())
                .trailStart(entity.getTrailStart())
                .trailEnd(entity.getTrailEnd())
                .location(entity.getLocation())
                .template(entity.getTemplate())
                .createdOn(entity.getCreatedOn())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }
}
