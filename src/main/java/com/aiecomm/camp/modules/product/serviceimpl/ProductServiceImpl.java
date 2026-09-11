package com.aiecomm.camp.modules.product.serviceimpl;

import com.aiecomm.camp.core.TenantContext;
import com.aiecomm.camp.modules.media.dto.ProductMediaDto;
import com.aiecomm.camp.modules.media.entity.AssetStatus;
import com.aiecomm.camp.modules.media.entity.MediaAsset;
import com.aiecomm.camp.modules.media.entity.MediaReference;
import com.aiecomm.camp.modules.media.entity.ProductImage;
import com.aiecomm.camp.modules.media.repository.MediaAssetRepository;
import com.aiecomm.camp.modules.media.repository.MediaReferenceRepository;
import com.aiecomm.camp.modules.media.repository.ProductImageRepository;
import com.aiecomm.camp.modules.product.dto.ProductCategoryDto;
import com.aiecomm.camp.modules.product.dto.ProductDto;
import com.aiecomm.camp.modules.product.entity.Product;
import com.aiecomm.camp.modules.product.entity.ProductCategory;
import com.aiecomm.camp.modules.product.repository.ProductCategoryRepository;
import com.aiecomm.camp.modules.product.repository.ProductRepository;
import com.aiecomm.camp.modules.product.service.ProductService;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.subscription.service.SubscriptionLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ProductImageRepository productImageRepository;
    private final MediaReferenceRepository mediaReferenceRepository;
    private final SubscriptionLimitService subscriptionLimitService;

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
        Product product = productRepository.findByProductIdAndStore_StoreId(productId, storeId).orElse(null);
        return ProductDto.fromEntity(product);
    }

    @Override
    public ProductDto createProduct(Long storeId, ProductDto dto) {
        log.info("Creating product for store: {}", storeId);

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        if (store.getTenant() == null || !tenantId.equals(store.getTenant().getTenantId())) {
            throw new AccessDeniedException("Store does not belong to the authenticated tenant");
        }

        // Resolve Category
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

        String sku = dto.getSku() != null && !dto.getSku().trim().isEmpty()
                ? dto.getSku().trim()
                : "SKU-" + System.currentTimeMillis() % 1000000;

        int stock = dto.getStockQuantity() != null ? dto.getStockQuantity() : 0;
        boolean inStock = dto.getInStock() != null ? dto.getInStock() : stock > 0;

        // Build product
        Product product = Product.builder()
                .productName(dto.getProductName() != null ? dto.getProductName() : "Untitled Product")
                .description(dto.getDescription())
                .productCategory(category)
                .sku(sku)
                .productSize(dto.getProductSize())
                .productColor(dto.getProductColor())
                .productPrice(dto.getProductPrice() != null ? dto.getProductPrice() : 0.0)
                .compareAtPrice(dto.getCompareAtPrice())
                .stockQuantity(stock)
                .inStock(inStock)
                .imageUrl(dto.getImageUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .store(store)
                .productImages(new ArrayList<>())
                .build();

        Product savedProduct = productRepository.save(product);

        // Attach pre-validated READY media assets (Zero cloud storage calls: pure fast PostgreSQL transaction)
        attachMediaAssetsToProduct(savedProduct, storeId, tenantId, dto.getImages());

        Product reloaded = productRepository.findById(savedProduct.getProductId()).orElse(savedProduct);
        log.info("Product created successfully with ID: {}", reloaded.getProductId());
        return ProductDto.fromEntity(reloaded);
    }

    @Override
    public ProductDto updateProduct(Long productId, Long storeId, ProductDto dto) {
        log.info("Updating product {} for store {}", productId, storeId);

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        Product product = productRepository.findByProductIdAndStore_StoreId(productId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (dto.getProductName() != null) product.setProductName(dto.getProductName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getProductPrice() != null) product.setProductPrice(dto.getProductPrice());
        if (dto.getCompareAtPrice() != null) product.setCompareAtPrice(dto.getCompareAtPrice());
        if (dto.getStockQuantity() != null) {
            product.setStockQuantity(dto.getStockQuantity());
            product.setInStock(dto.getStockQuantity() > 0);
        }
        if (dto.getSku() != null) product.setSku(dto.getSku());
        if (dto.getProductSize() != null) product.setProductSize(dto.getProductSize());
        if (dto.getProductColor() != null) product.setProductColor(dto.getProductColor());
        if (dto.getImageUrl() != null) product.setImageUrl(dto.getImageUrl());
        if (dto.getStatus() != null) product.setStatus(dto.getStatus());

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

        // If images list was provided in the update payload
        if (dto.getImages() != null) {
            // Detach existing references
            mediaReferenceRepository.deleteByReferenceableTypeAndReferenceableId("product", productId);
            product.getProductImages().clear();
            productRepository.saveAndFlush(product);

            attachMediaAssetsToProduct(product, storeId, tenantId, dto.getImages());
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
            // Clean up generic media references for this product
            mediaReferenceRepository.deleteByReferenceableTypeAndReferenceableId("product", productId);
            productRepository.delete(productOpt.get());
            log.info("Product {} deleted successfully. Underlying media assets remain untouched.", productId);
            return true;
        }
        return false;
    }

    @Override
    public ProductDto attachImages(Long productId, Long storeId, List<Long> mediaAssetIds) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }

        Product product = productRepository.findByProductIdAndStore_StoreId(productId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (mediaAssetIds != null && !mediaAssetIds.isEmpty()) {
            int currentCount = product.getProductImages().size();
            subscriptionLimitService.validateProductImageCount(tenantId, currentCount + mediaAssetIds.size());

            int nextSortOrder = currentCount;
            for (Long assetId : mediaAssetIds) {
                // 1. Validate ownership: reject with IllegalArgumentException/404 if not found in this store
                MediaAsset asset = mediaAssetRepository.findByIdAndStore_StoreIdAndTenant_TenantId(assetId, storeId, tenantId)
                        .orElseThrow(() -> new IllegalArgumentException("Media asset not found in store: " + assetId));

                // 2. Assert status == READY
                if (asset.getStatus() != AssetStatus.READY) {
                    throw new IllegalArgumentException("Media asset " + assetId + " is not ready yet (status: " + asset.getStatus() + ")");
                }

                boolean isPrimary = product.getProductImages().isEmpty() && (nextSortOrder == 0);
                ProductImage pi = ProductImage.builder()
                        .product(product)
                        .mediaAsset(asset)
                        .sortOrder(nextSortOrder++)
                        .isPrimary(isPrimary)
                        .build();

                product.getProductImages().add(pi);

                // Add to reference ledger
                if (!mediaReferenceRepository.existsByMediaAsset_IdAndReferenceableTypeAndReferenceableId(assetId, "product", productId)) {
                    mediaReferenceRepository.save(MediaReference.builder()
                            .mediaAsset(asset)
                            .referenceableType("product")
                            .referenceableId(productId)
                            .build());
                }

                if (isPrimary || product.getImageUrl() == null) {
                    product.setImageUrl(asset.getPublicUrl());
                }
            }

            productRepository.save(product);
        }

        return ProductDto.fromEntity(product);
    }

    @Override
    public ProductDto detachImage(Long productId, Long storeId, Long imageId) {
        Product product = productRepository.findByProductIdAndStore_StoreId(productId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        Optional<ProductImage> targetImg = product.getProductImages().stream()
                .filter(pi -> pi.getId().equals(imageId))
                .findFirst();

        if (targetImg.isPresent()) {
            ProductImage pi = targetImg.get();
            Long assetId = pi.getMediaAsset().getId();

            product.getProductImages().remove(pi);

            // Remove from reference ledger if no other image on this product uses this asset
            boolean stillUsedOnProduct = product.getProductImages().stream()
                    .anyMatch(other -> other.getMediaAsset().getId().equals(assetId));
            if (!stillUsedOnProduct) {
                mediaReferenceRepository.deleteByReferenceableTypeAndReferenceableId("product", productId);
            }

            // Update primary if removed image was primary
            if (Boolean.TRUE.equals(pi.getIsPrimary()) && !product.getProductImages().isEmpty()) {
                ProductImage newPrimary = product.getProductImages().get(0);
                newPrimary.setIsPrimary(true);
                product.setImageUrl(newPrimary.getMediaAsset().getPublicUrl());
            } else if (product.getProductImages().isEmpty()) {
                product.setImageUrl(null);
            }

            productRepository.save(product);
        }

        return ProductDto.fromEntity(product);
    }

    private void attachMediaAssetsToProduct(Product product, Long storeId, UUID tenantId, List<ProductMediaDto> images) {
        if (images == null || images.isEmpty()) return;

        subscriptionLimitService.validateProductImageCount(tenantId, images.size());

        String primaryUrl = null;
        boolean hasPrimaryAssigned = false;

        for (int i = 0; i < images.size(); i++) {
            ProductMediaDto imgDto = images.get(i);
            Long assetId = imgDto.getMediaAssetId();
            if (assetId == null) continue;

            // 1. Validate ownership: reject with IllegalArgumentException/404 if not in caller scope
            MediaAsset asset = mediaAssetRepository.findByIdAndStore_StoreIdAndTenant_TenantId(assetId, storeId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Media asset not found in store: " + assetId));

            // 2. Assert status == READY
            if (asset.getStatus() != AssetStatus.READY) {
                throw new IllegalArgumentException("Media asset " + assetId + " is not ready yet (status: " + asset.getStatus() + ")");
            }

            boolean isPrimary = (!hasPrimaryAssigned && (Boolean.TRUE.equals(imgDto.getIsPrimary()) || i == 0));
            if (isPrimary) {
                hasPrimaryAssigned = true;
            }

            ProductImage pi = ProductImage.builder()
                    .product(product)
                    .mediaAsset(asset)
                    .sortOrder(imgDto.getSortOrder() != null ? imgDto.getSortOrder() : i)
                    .isPrimary(isPrimary)
                    .build();

            product.getProductImages().add(pi);

            // 3. Write to generic reference ledger (media_references)
            if (!mediaReferenceRepository.existsByMediaAsset_IdAndReferenceableTypeAndReferenceableId(assetId, "product", product.getProductId())) {
                mediaReferenceRepository.save(MediaReference.builder()
                        .mediaAsset(asset)
                        .referenceableType("product")
                        .referenceableId(product.getProductId())
                        .build());
            }

            if (isPrimary) {
                primaryUrl = asset.getPublicUrl();
            }
        }

        if (primaryUrl != null) {
            product.setImageUrl(primaryUrl);
        }
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
