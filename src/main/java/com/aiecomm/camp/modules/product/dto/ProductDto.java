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
    private Long categoryId;
    private String categoryName;
    private String sku;
    private String productSize;
    private String productColor;
    private Double productPrice;
    private Boolean inStock;
    private Long storeId;
    private Instant createdAt;

    public static ProductDto fromEntity(Product entity) {
        if (entity == null) return null;
        return ProductDto.builder()
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .categoryId(entity.getProductCategory() != null ? entity.getProductCategory().getCategoryId() : null)
                .categoryName(entity.getProductCategory() != null ? entity.getProductCategory().getCategoryName() : null)
                .sku(entity.getSku())
                .productSize(entity.getProductSize())
                .productColor(entity.getProductColor())
                .productPrice(entity.getProductPrice())
                .inStock(entity.getInStock())
                .storeId(entity.getStore() != null ? entity.getStore().getStoreId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
