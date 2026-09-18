package com.aiecomm.camp.modules.commerce.cart;

import com.aiecomm.camp.modules.commerce.cart.entity.Cart;
import com.aiecomm.camp.modules.commerce.cart.entity.CartItem;
import com.aiecomm.camp.modules.commerce.context.CommerceContext;
import com.aiecomm.camp.modules.commerce.discount.DiscountService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Stateless in-memory cart calculator.
 *
 * Operates entirely on the already-loaded Cart and CartItem objects.
 * Makes ZERO additional database calls.
 *
 * This is the single authoritative source for cart totals.
 * The frontend never recalculates these values.
 */
public final class CartCalculator {

    private CartCalculator() {}

    /**
     * Recalculate all line totals and cart totals in memory.
     * Mutates the cart and item objects in place.
     *
     * @param cart            the cart to recalculate
     * @param discountService discount strategy (NoOp in M1)
     * @param ctx             commerce context for discount resolution
     */
    public static void recalculate(Cart cart, DiscountService discountService, CommerceContext ctx) {
        List<CartItem> items = cart.getItems();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : items) {
            // line subtotal = unitPrice × quantity
            BigDecimal lineSubtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(4, RoundingMode.HALF_UP);

            // line discount (defaults to ZERO if null or NoOp)
            BigDecimal rawDiscount = discountService != null
                    ? discountService.calculateLineDiscount(item, ctx)
                    : null;
            BigDecimal discount = (rawDiscount != null ? rawDiscount : BigDecimal.ZERO)
                    .setScale(4, RoundingMode.HALF_UP);

            // line total = subtotal - discount, minimum 0
            BigDecimal lineTotal = lineSubtotal.subtract(discount).max(BigDecimal.ZERO)
                    .setScale(4, RoundingMode.HALF_UP);

            item.setLineSubtotal(lineSubtotal);
            item.setDiscountAmount(discount);
            item.setLineTotal(lineTotal);

            subtotal = subtotal.add(lineSubtotal);
        }

        // Cart-level discount (defaults to ZERO if null or NoOp)
        BigDecimal rawCartDiscount = discountService != null
                ? discountService.calculateCartDiscount(cart, ctx)
                : null;
        BigDecimal cartDiscount = (rawCartDiscount != null ? rawCartDiscount : BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);

        // Shipping and tax are stubs in M1
        BigDecimal shipping = cart.getShippingTotal() != null
                ? cart.getShippingTotal()
                : BigDecimal.ZERO;
        BigDecimal tax = cart.getTaxTotal() != null
                ? cart.getTaxTotal()
                : BigDecimal.ZERO;

        BigDecimal grandTotal = subtotal
                .subtract(cartDiscount)
                .add(shipping)
                .add(tax)
                .max(BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);

        cart.setSubtotal(subtotal.setScale(4, RoundingMode.HALF_UP));
        cart.setDiscountTotal(cartDiscount);
        cart.setShippingTotal(shipping);
        cart.setTaxTotal(tax);
        cart.setGrandTotal(grandTotal);
        cart.setUpdatedAt(Instant.now());
    }
}
