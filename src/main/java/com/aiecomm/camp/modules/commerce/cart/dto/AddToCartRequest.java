package com.aiecomm.camp.modules.commerce.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for POST /api/storefront/cart/items
 * storeId is intentionally absent — it is derived from the request host.
 */
public record AddToCartRequest(
        @NotNull(message = "productId is required")
        @Positive(message = "productId must be a positive number")
        Long productId,

        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 999, message = "quantity cannot exceed 999")
        int quantity
) {}
