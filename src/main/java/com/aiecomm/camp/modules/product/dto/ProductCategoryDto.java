package com.aiecomm.camp.modules.product.dto;

import com.aiecomm.camp.modules.product.entity.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryDto {

    private Long categoryId;
    private String categoryName;

    public static ProductCategoryDto fromEntity(ProductCategory entity) {
        if (entity == null) return null;
        return ProductCategoryDto.builder()
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategoryName())
                .build();
    }
}
