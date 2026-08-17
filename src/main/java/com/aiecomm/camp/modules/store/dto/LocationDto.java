package com.aiecomm.camp.modules.store.dto;

import com.aiecomm.camp.modules.store.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDto {

    private Long locationId;
    private String locationName;
    private Long storeId;
    private String locationType;
    private Double latitude;
    private Double longitude;
    private String status;
    private String storeType;
    private Instant createdAt;
    private Instant updatedAt;

    public static LocationDto fromEntity(Location entity) {
        if (entity == null) return null;
        return LocationDto.builder()
                .locationId(entity.getLocationId())
                .locationName(entity.getLocationName())
                .storeId(entity.getStore() != null ? entity.getStore().getStoreId() : null)
                .locationType(entity.getLocationType())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .status(entity.getStatus())
                .storeType(entity.getStoreType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
