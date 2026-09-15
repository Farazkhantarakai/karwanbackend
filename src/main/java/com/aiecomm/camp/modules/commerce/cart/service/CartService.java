package com.aiecomm.camp.modules.commerce.cart.service;

import com.aiecomm.camp.modules.commerce.cart.dto.CartResponse;
import com.aiecomm.camp.modules.commerce.context.CommerceContext;

/**
 * CartService — the authoritative commerce boundary for all cart operations.
 *
 * All methods receive a CommerceContext derived from the request host + session hash.
 * The client never supplies storeId, tenantId, or price — the server derives these.
 */
public interface CartService {

    /** Get or create the active cart for the current session. */
    CartResponse getOrCreateCart(CommerceContext ctx);

    /** Add a product to the cart. Returns the updated full CartResponse. */
    CartResponse addItem(CommerceContext ctx, Long productId, int quantity);

    /**
     * Update quantity of an existing item.
     * quantity=0 removes the item (equivalent to removeItem).
     * C5: Verifies item belongs to ctx.storeId() before mutating.
     */
    CartResponse updateItemQuantity(CommerceContext ctx, Long itemId, int quantity);

    /**
     * Remove an item from the cart.
     * C5: Verifies item belongs to ctx.storeId() before deleting.
     */
    CartResponse removeItem(CommerceContext ctx, Long itemId);

    /** Clear all items from the cart (keep the cart record). */
    CartResponse clearCart(CommerceContext ctx);
}
