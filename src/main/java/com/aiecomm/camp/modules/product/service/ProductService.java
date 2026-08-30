package com.aiecomm.camp.modules.product.service;

import com.aiecomm.camp.modules.product.dto.ProductCategoryDto;
import com.aiecomm.camp.modules.product.dto.ProductDto;

import java.util.List;

public interface ProductService {

    List<ProductDto> getProductsByStore(Long storeId);

    List<ProductDto> getActiveProductsByStore(Long storeId);

    ProductDto getProductById(Long productId, Long storeId);

    ProductDto createProduct(Long storeId, ProductDto dto);

    ProductDto updateProduct(Long productId, Long storeId, ProductDto dto);

    boolean deleteProduct(Long productId, Long storeId);

    List<ProductCategoryDto> getAllCategories();

    ProductCategoryDto createCategory(String categoryName);
}
