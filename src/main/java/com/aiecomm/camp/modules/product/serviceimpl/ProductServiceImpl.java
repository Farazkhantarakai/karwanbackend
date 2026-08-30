package com.aiecomm.camp.modules.product.serviceimpl;

import com.aiecomm.camp.modules.product.dto.ProductCategoryDto;
import com.aiecomm.camp.modules.product.dto.ProductDto;
import com.aiecomm.camp.modules.product.entity.Product;
import com.aiecomm.camp.modules.product.entity.ProductCategory;
import com.aiecomm.camp.modules.product.repository.ProductCategoryRepository;
import com.aiecomm.camp.modules.product.repository.ProductRepository;
import com.aiecomm.camp.modules.product.service.ProductService;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByStore(Long storeId) {
        log.info("Fetching all products for storeId: {}", storeId);
        return productRepository.findByStore_StoreId(storeId).stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getActiveProductsByStore(Long storeId) {
        log.info("Fetching active in-stock products for storeId: {}", storeId);
        return productRepository.findByStore_StoreIdAndInStockTrue(storeId).stream()
                .map(ProductDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long productId, Long storeId) {
        log.info("Fetching product {} for store {}", productId, storeId);
        return productRepository.findByProductIdAndStore_StoreId(productId, storeId)
                .map(ProductDto::fromEntity)
                .orElse(null);
    }

    @Override
    public ProductDto createProduct(Long storeId, ProductDto dto) {
        log.info("Creating product '{}' for storeId: {}", dto.getProductName(), storeId);

        // Find Store
        Optional<?> optionalStore = storeRepository.findById(storeId);
        if (optionalStore.isEmpty()) {
            throw new IllegalArgumentException("Store not found with id: " + storeId);
        }
        Store store = (Store) optionalStore.get();

        // Resolve Category if provided
        ProductCategory category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
        } else if (dto.getCategoryName() != null && !dto.getCategoryName().trim().isEmpty()) {
            category = categoryRepository.findByCategoryNameIgnoreCase(dto.getCategoryName().trim())
                    .orElseGet(() -> categoryRepository.save(
                            ProductCategory.builder()
                                    .categoryName(dto.getCategoryName().trim())
                                    .build()
                    ));
        }

        // Auto-generate SKU if not present
        String sku = dto.getSku();
        if (sku == null || sku.trim().isEmpty()) {
            sku = "SKU-" + System.currentTimeMillis() % 1000000;
        }

        Integer stock = dto.getStockQuantity() != null ? dto.getStockQuantity() : 0;
        Boolean inStock = dto.getInStock() != null ? dto.getInStock() : (stock > 0);

        Product product = Product.builder()
                .productName(dto.getProductName())
                .description(dto.getDescription())
                .productCategory(category)
                .sku(sku)
                .productSize(dto.getProductSize())
                .productColor(dto.getProductColor())
                .productPrice(dto.getProductPrice())
                .compareAtPrice(dto.getCompareAtPrice())
                .stockQuantity(stock)
                .inStock(inStock)
                .imageUrl(dto.getImageUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .store(store)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getProductId());
        return ProductDto.fromEntity(savedProduct);
    }

    @Override
    public ProductDto updateProduct(Long productId, Long storeId, ProductDto dto) {
        log.info("Updating product {} for store {}", productId, storeId);

        Product product = productRepository.findByProductIdAndStore_StoreId(productId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        if (dto.getProductName() != null) product.setProductName(dto.getProductName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getSku() != null) product.setSku(dto.getSku());
        if (dto.getProductSize() != null) product.setProductSize(dto.getProductSize());
        if (dto.getProductColor() != null) product.setProductColor(dto.getProductColor());
        if (dto.getProductPrice() != null) product.setProductPrice(dto.getProductPrice());
        if (dto.getCompareAtPrice() != null) product.setCompareAtPrice(dto.getCompareAtPrice());
        if (dto.getImageUrl() != null) product.setImageUrl(dto.getImageUrl());
        if (dto.getStatus() != null) product.setStatus(dto.getStatus());

        if (dto.getStockQuantity() != null) {
            product.setStockQuantity(dto.getStockQuantity());
            product.setInStock(dto.getStockQuantity() > 0);
        } else if (dto.getInStock() != null) {
            product.setInStock(dto.getInStock());
        }

        // Update category if provided
        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId()).ifPresent(product::setProductCategory);
        } else if (dto.getCategoryName() != null && !dto.getCategoryName().trim().isEmpty()) {
            ProductCategory cat = categoryRepository.findByCategoryNameIgnoreCase(dto.getCategoryName().trim())
                    .orElseGet(() -> categoryRepository.save(
                            ProductCategory.builder()
                                    .categoryName(dto.getCategoryName().trim())
                                    .build()
                    ));
            product.setProductCategory(cat);
        }

        Product updated = productRepository.save(product);
        log.info("Product updated successfully: {}", updated.getProductId());
        return ProductDto.fromEntity(updated);
    }

    @Override
    public boolean deleteProduct(Long productId, Long storeId) {
        log.info("Deleting product {} for store {}", productId, storeId);
        Optional<Product> productOpt = productRepository.findByProductIdAndStore_StoreId(productId, storeId);
        if (productOpt.isPresent()) {
            productRepository.delete(productOpt.get());
            log.info("Product {} deleted successfully", productId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(ProductCategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ProductCategoryDto createCategory(String categoryName) {
        String cleanName = categoryName != null ? categoryName.trim() : "";
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        ProductCategory category = categoryRepository.findByCategoryNameIgnoreCase(cleanName)
                .orElseGet(() -> categoryRepository.save(
                        ProductCategory.builder()
                                .categoryName(cleanName)
                                .build()
                ));

        return ProductCategoryDto.fromEntity(category);
    }
}
