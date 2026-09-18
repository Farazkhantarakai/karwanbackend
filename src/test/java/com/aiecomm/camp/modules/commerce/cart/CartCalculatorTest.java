package com.aiecomm.camp.modules.commerce.cart;

import com.aiecomm.camp.modules.commerce.cart.entity.Cart;
import com.aiecomm.camp.modules.commerce.cart.entity.CartItem;
import com.aiecomm.camp.modules.commerce.context.CommerceContext;
import com.aiecomm.camp.modules.commerce.context.StoreContext;
import com.aiecomm.camp.modules.commerce.discount.NoOpDiscountService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartCalculatorTest {

    @Test
    void testRecalculateEmptyCart() {
        Cart cart = Cart.builder()
                .items(new ArrayList<>())
                .build();

        CommerceContext ctx = new CommerceContext(
                new StoreContext(1L, UUID.randomUUID(), "PKR", "PK", "test-store"),
                "hash",
                null
        );

        CartCalculator.recalculate(cart, new NoOpDiscountService(), ctx);

        assertEquals(new BigDecimal("0.0000"), cart.getSubtotal());
        assertEquals(new BigDecimal("0.0000"), cart.getDiscountTotal());
        assertEquals(new BigDecimal("0.0000"), cart.getGrandTotal());
    }

    @Test
    void testRecalculateCartWithItems() {
        CartItem item1 = CartItem.builder()
                .productId(101L)
                .quantity(2)
                .unitPrice(new BigDecimal("1500.5000"))
                .build();

        CartItem item2 = CartItem.builder()
                .productId(102L)
                .quantity(1)
                .unitPrice(new BigDecimal("299.9900"))
                .build();

        Cart cart = Cart.builder()
                .items(new ArrayList<>(java.util.List.of(item1, item2)))
                .build();

        CommerceContext ctx = new CommerceContext(
                new StoreContext(1L, UUID.randomUUID(), "PKR", "PK", "test-store"),
                "hash",
                null
        );

        CartCalculator.recalculate(cart, new NoOpDiscountService(), ctx);

        // item1 lineSubtotal = 1500.5000 * 2 = 3001.0000
        assertEquals(new BigDecimal("3001.0000"), item1.getLineSubtotal());
        assertEquals(new BigDecimal("3001.0000"), item1.getLineTotal());

        // item2 lineSubtotal = 299.9900 * 1 = 299.9900
        assertEquals(new BigDecimal("299.9900"), item2.getLineSubtotal());
        assertEquals(new BigDecimal("299.9900"), item2.getLineTotal());

        // Cart subtotal = 3001.0000 + 299.9900 = 3300.9900
        assertEquals(new BigDecimal("3300.9900"), cart.getSubtotal());
        assertEquals(new BigDecimal("0.0000"), cart.getDiscountTotal());
        assertEquals(new BigDecimal("3300.9900"), cart.getGrandTotal());
    }
}
