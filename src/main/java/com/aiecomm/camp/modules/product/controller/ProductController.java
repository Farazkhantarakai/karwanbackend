package com.aiecomm.camp.modules.product.controller;

import com.aiecomm.camp.common.dto.ApiResponse;
import com.aiecomm.camp.modules.product.dto.ProductCategoryDto;
import com.aiecomm.camp.modules.product.dto.ProductDto;
import com.aiecomm.camp.modules.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1", "/api"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProductController {

    private final ProductService productService;

    @GetMapping("/stores/{storeId}/products")
    public ApiResponse<List<ProductDto>> getProductsByStore(@PathVariable Long storeId) {
        log.info("REST: GET /api/v1/stores/{}/products", storeId);
        List<ProductDto> products = productService.getProductsByStore(storeId);
        return ApiResponse.success("Products fetched successfully", products);
    }

    @GetMapping("/stores/{storeId}/products/active")
    public ApiResponse<List<ProductDto>> getActiveProductsByStore(@PathVariable Long storeId) {
        log.info("REST: GET /api/v1/stores/{}/products/active", storeId);
        List<ProductDto> products = productService.getActiveProductsByStore(storeId);
        return ApiResponse.success("Active products fetched successfully", products);
    }

    @GetMapping("/stores/{storeId}/products/{productId}")
    public ApiResponse<ProductDto> getProductById(
            @PathVariable Long storeId,
            @PathVariable Long productId) {
        log.info("REST: GET /api/v1/stores/{}/products/{}", storeId, productId);
        ProductDto product = productService.getProductById(productId, storeId);
        if (product != null) {
            return ApiResponse.success("Product found", product);
        }
        return ApiResponse.error("Product not found");
    }

    @PostMapping("/stores/{storeId}/products")
    public ApiResponse<ProductDto> createProduct(
            @PathVariable Long storeId,
            @RequestBody ProductDto productDto) {
        log.info("REST: POST /api/v1/stores/{}/products", storeId);
        try {
            ProductDto created = productService.createProduct(storeId, productDto);
            return ApiResponse.success("Product created successfully", created);
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating product: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating product", e);
            return ApiResponse.error("Failed to create product: " + e.getMessage());
        }
    }

    @PutMapping("/stores/{storeId}/products/{productId}")
    public ApiResponse<ProductDto> updateProduct(
            @PathVariable Long storeId,
            @PathVariable Long productId,
            @RequestBody ProductDto productDto) {
        log.info("REST: PUT /api/v1/stores/{}/products/{}", storeId, productId);
        try {
            ProductDto updated = productService.updateProduct(productId, storeId, productDto);
            return ApiResponse.success("Product updated successfully", updated);
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating product: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating product", e);
            return ApiResponse.error("Failed to update product: " + e.getMessage());
        }
    }

    @DeleteMapping("/stores/{storeId}/products/{productId}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable Long storeId,
            @PathVariable Long productId) {
        log.info("REST: DELETE /api/v1/stores/{}/products/{}", storeId, productId);
        boolean deleted = productService.deleteProduct(productId, storeId);
        if (deleted) {
            return ApiResponse.success("Product deleted successfully");
        }
        return ApiResponse.error("Product not found or could not be deleted");
    }

    @GetMapping("/categories")
    public ApiResponse<List<ProductCategoryDto>> getAllCategories() {
        log.info("REST: GET /api/v1/categories");
        List<ProductCategoryDto> categories = productService.getAllCategories();
        return ApiResponse.success("Categories fetched successfully", categories);
    }

    @PostMapping("/categories")
    public ApiResponse<ProductCategoryDto> createCategory(@RequestBody ProductCategoryDto request) {
        log.info("REST: POST /api/v1/categories");
        try {
            ProductCategoryDto category = productService.createCategory(request.getCategoryName());
            return ApiResponse.success("Category created successfully", category);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
