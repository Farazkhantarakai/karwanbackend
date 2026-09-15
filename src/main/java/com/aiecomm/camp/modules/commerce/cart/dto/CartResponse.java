package com.aiecomm.camp.modules.commerce.cart.dto;

import java.util.List;

/**
 * Full cart response sent to the frontend.
 *
 * All monetary values are formatted strings (e.g. "2499.0000") — never Double or float.
 * The frontend displays these strings directly. It MUST NOT parseFloat() for arithmetic.
 * Cart totals are server-authoritative — the frontend never recalculates them.
 */
public record CartResponse(
        String id,
        String currency,
        List<CartItemResponse> items,
        CartSummaryResponse summary,
        int itemCount       // total unit count (sum of all quantities)
) {}
