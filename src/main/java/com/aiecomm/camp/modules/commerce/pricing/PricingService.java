package com.aiecomm.camp.modules.commerce.pricing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Authoritative price resolver for the commerce engine.
 *
 * This is the ONLY place in the codebase where product prices are read.
 * All internal commerce logic (cart, checkout, order) uses BigDecimal from this service.
 *
 * Current implementation converts Product.productPrice (Double, legacy) to BigDecimal.
 * Future migration: when products.product_price column becomes NUMERIC(19,4),
 * only this implementation changes — the interface and all callers stay the same.
 */

public interface PricingService {

    /**
     * Resolve the authoritative sell price for a product in a given store.
     * Always returns BigDecimal. Never returns null.
     *
     * @param productId the product to price
     * @param storeId   the store context (for store-specific pricing in future)
     * @return BigDecimal price, scale 4, HALF_UP
     * @throws com.aiecomm.camp.modules.commerce.exception.CommerceException PRODUCT_NOT_FOUND
     */
    BigDecimal resolvePrice(Long productId, Long storeId);
}
