package com.aiecomm.camp.modules.product.dto;

import com.aiecomm.camp.modules.media.dto.ProductMediaDto;
import com.aiecomm.camp.modules.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    private Long productId;
    private String productName;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String sku;
    private String productSize;
    private String productColor;
    private Double productPrice;
    private Double compareAtPrice;
    private Integer stockQuantity;
    private Boolean inStock;
    private String imageUrl;
    private String status;
    private Long storeId;
    private Instant createdAt;
    private Instant updatedAt;

    private List<UUID> mediaIds;
    private List<ProductMediaDto> images;

    public static ProductDto fromEntity(Product entity) {
        return fromEntity(entity, null);
    }

    public static ProductDto fromEntity(Product entity, String publicDomain) {
        if (entity == null) return null;

        List<ProductMediaDto> imagesList = Collections.emptyList();
        if (entity.getProductImages() != null && !entity.getProductImages().isEmpty()) {
            imagesList = entity.getProductImages().stream()
                    .map(ProductMediaDto::fromProductImage)
                    .collect(Collectors.toList());
        }

        return ProductDto.builder()
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .description(entity.getDescription())
                .categoryId(entity.getProductCategory() != null ? entity.getProductCategory().getCategoryId() : null)
                .categoryName(entity.getProductCategory() != null ? entity.getProductCategory().getCategoryName() : null)
                .sku(entity.getSku())
                .productSize(entity.getProductSize())
                .productColor(entity.getProductColor())
                .productPrice(entity.getProductPrice())
                .compareAtPrice(entity.getCompareAtPrice())
                .stockQuantity(entity.getStockQuantity())
                .inStock(entity.getInStock())
                .imageUrl(entity.getImageUrl())
                .status(entity.getStatus())
                .storeId(entity.getStore() != null ? entity.getStore().getStoreId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .images(imagesList)
                .build();
    }
}
