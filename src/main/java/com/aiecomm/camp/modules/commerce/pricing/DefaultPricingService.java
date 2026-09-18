package com.aiecomm.camp.modules.commerce.pricing;

import com.aiecomm.camp.modules.commerce.exception.CommerceException;
import com.aiecomm.camp.modules.product.entity.Product;
import com.aiecomm.camp.modules.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Default implementation of PricingService.
 * Acts as the single boundary converting Product.productPrice (Double, legacy) to BigDecimal.
 */
@Service
@RequiredArgsConstructor
public class DefaultPricingService implements PricingService {

    private final ProductRepository productRepository;

    @Override
    public BigDecimal resolvePrice(Long productId, Long storeId) {
        Product product = productRepository
                .findActiveByProductIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new CommerceException("PRODUCT_NOT_FOUND",
                        "Product not found or inactive for store: " + storeId));

        Double rawPrice = product.getProductPrice();
        if (rawPrice == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(rawPrice).setScale(4, RoundingMode.HALF_UP);
    }
}
