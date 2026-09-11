package com.aiecomm.camp.modules.media.dto;

import com.aiecomm.camp.modules.media.entity.AssetStatus;
import com.aiecomm.camp.modules.media.entity.MediaAsset;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetDto {
    private Long id;
    private Long storeId;
    private String objectKey;
    private String publicUrl;
    private String originalFilename;
    private String contentType;
    private Long size;
    private Integer width;
    private Integer height;
    private AssetStatus status;
    private String errorMessage;
    private Instant createdAt;
    private Instant readyAt;

    public static MediaAssetDto fromEntity(MediaAsset asset) {
        if (asset == null) return null;
        return MediaAssetDto.builder()
                .id(asset.getId())
                .storeId(asset.getStore() != null ? asset.getStore().getStoreId() : null)
                .objectKey(asset.getObjectKey())
                .publicUrl(asset.getPublicUrl())
                .originalFilename(asset.getOriginalFilename())
                .contentType(asset.getContentType())
                .size(asset.getSize())
                .width(asset.getWidth())
                .height(asset.getHeight())
                .status(asset.getStatus())
                .errorMessage(asset.getErrorMessage())
                .createdAt(asset.getCreatedAt())
                .readyAt(asset.getReadyAt())
                .build();
    }
}
