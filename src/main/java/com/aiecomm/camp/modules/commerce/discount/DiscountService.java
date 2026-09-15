package com.aiecomm.camp.modules.commerce.discount;

import com.aiecomm.camp.modules.commerce.cart.entity.Cart;
import com.aiecomm.camp.modules.commerce.cart.entity.CartItem;
import com.aiecomm.camp.modules.commerce.context.CommerceContext;

import java.math.BigDecimal;

/**
 * Service interface for calculating cart discounts.
 */
public interface DiscountService {

    /**
     * Calculates discount for an individual line item.
     */
    BigDecimal calculateLineDiscount(CartItem item, CommerceContext ctx);

    /**
     * Calculates overall cart-level discount.
     */
    BigDecimal calculateCartDiscount(Cart cart, CommerceContext ctx);
}
