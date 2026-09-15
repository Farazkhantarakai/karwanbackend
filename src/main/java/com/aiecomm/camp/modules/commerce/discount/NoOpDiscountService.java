package com.aiecomm.camp.modules.commerce.discount;

import com.aiecomm.camp.modules.commerce.context.CommerceContext;
import com.aiecomm.camp.modules.commerce.cart.entity.Cart;
import com.aiecomm.camp.modules.commerce.cart.entity.CartItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * No-op discount service for Milestone 1.
 * No discounts are applied in the guest cart phase.
 *
 * Replace with PromoCodeDiscountService when discount features are built (M2+).
 */
@Service
public class NoOpDiscountService implements DiscountService {

    @Override
    public BigDecimal calculateLineDiscount(CartItem item, CommerceContext ctx) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateCartDiscount(Cart cart, CommerceContext ctx) {
        return BigDecimal.ZERO;
    }
}
