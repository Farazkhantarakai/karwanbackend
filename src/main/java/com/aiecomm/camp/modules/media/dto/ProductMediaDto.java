package com.aiecomm.camp.modules.media.dto;

import com.aiecomm.camp.modules.media.entity.ProductImage;
import com.aiecomm.camp.modules.media.entity.ProductMedia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMediaDto {
    private Long id;
    private Long mediaAssetId;
    private UUID mediaId;
    private String url;
    private Integer sortOrder;
    private Boolean isPrimary;
    private String originalFilename;
    private String contentType;
    private Integer width;
    private Integer height;
    private Long size;

    public static ProductMediaDto fromProductImage(ProductImage pi) {
        if (pi == null || pi.getMediaAsset() == null) return null;
        var asset = pi.getMediaAsset();
        return ProductMediaDto.builder()
                .id(pi.getId())
                .mediaAssetId(asset.getId())
                .url(asset.getPublicUrl())
                .sortOrder(pi.getSortOrder())
                .isPrimary(pi.getIsPrimary())
                .originalFilename(asset.getOriginalFilename())
                .contentType(asset.getContentType())
                .width(asset.getWidth())
                .height(asset.getHeight())
                .size(asset.getSize())
                .build();
    }

    public static ProductMediaDto fromEntity(ProductMedia pm, String publicBaseUrl) {
        if (pm == null || pm.getMedia() == null) return null;
        var media = pm.getMedia();

        String url;
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String trimmed = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            url = trimmed + "/" + media.getObjectKey();
        } else {
            url = "/" + media.getObjectKey();
        }

        return ProductMediaDto.builder()
                .mediaId(media.getId())
                .url(url)
                .sortOrder(pm.getSortOrder())
                .isPrimary(pm.getIsPrimary())
                .originalFilename(media.getOriginalFilename())
                .contentType(media.getContentType())
                .width(media.getWidth())
                .height(media.getHeight())
                .size(media.getSize())
                .build();
    }
}
