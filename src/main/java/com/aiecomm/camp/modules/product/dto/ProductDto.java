package com.aiecomm.camp.modules.product.dto;

import com.aiecomm.camp.modules.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    public static ProductDto fromEntity(Product entity) {
        if (entity == null) return null;
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
                .build();
    }
}
