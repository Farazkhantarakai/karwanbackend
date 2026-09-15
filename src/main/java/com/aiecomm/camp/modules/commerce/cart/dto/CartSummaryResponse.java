package com.aiecomm.camp.modules.commerce.cart.dto;

/**
 * Cart totals summary. All values are formatted decimal strings.
 * grand_total is the authoritative total — the frontend must display it as-is,
 * never recompute it from items.
 */
public record CartSummaryResponse(
        String subtotal,
        String discountTotal,
        String shippingTotal,
        String taxTotal,
        String grandTotal    // authoritative from server — never recalculate client-side
) {}
