package com.aiecomm.camp.modules.commerce.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request body for PATCH /api/storefront/cart/items/{itemId}
 * Setting quantity to 0 removes the item from the cart.
 */
public record UpdateCartItemRequest(
        @Min(value = 0, message = "quantity cannot be negative")
        @Max(value = 999, message = "quantity cannot exceed 999")
        int quantity
) {}
