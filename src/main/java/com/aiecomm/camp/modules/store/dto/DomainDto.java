package com.aiecomm.camp.modules.store.dto;

import com.aiecomm.camp.modules.store.entity.Domain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainDto {

    private String name;
    private Long storeId;
    private String domainlink;
    private String sslStatus;
    private Boolean isPrimary;
    private String domainStatus;
    private Instant createdOn;
    private Instant updatedOn;

    public static DomainDto fromEntity(Domain entity) {
        if (entity == null) return null;
        return DomainDto.builder()
//                .domainId(entity.getDomainId())
                .name(entity.getName())
                .storeId(entity.getStore() != null ? entity.getStore().getStoreId() : null)
                .domainlink(entity.getDomainlink())
                .sslStatus(String.valueOf(entity.getSslstatus()))
                .isPrimary(entity.getIsPrimary())
                .domainStatus(String.valueOf(entity.getDomainStatus()))
                .createdOn(entity.getCreatedOn())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }
}
