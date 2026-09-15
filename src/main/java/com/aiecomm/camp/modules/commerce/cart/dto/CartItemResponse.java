package com.aiecomm.camp.modules.commerce.cart.dto;

/**
 * Line item response inside a CartResponse.
 * All monetary amounts are formatted strings (never Double).
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String title,
        String sku,
        String imageUrl,
        int quantity,
        String unitPrice,
        String lineTotal
) {}
