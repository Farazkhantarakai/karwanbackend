package com.aiecomm.camp.modules.commerce.cart;

import com.aiecomm.camp.modules.commerce.cart.dto.*;
import com.aiecomm.camp.modules.commerce.cart.entity.Cart;
import com.aiecomm.camp.modules.commerce.cart.entity.CartItem;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Maps Cart domain objects to CartResponse DTOs.
 * All BigDecimal values are formatted as plain decimal strings (toPlainString()).
 * The frontend receives strings and displays them — no floating point involved.
 */
public final class CartMapper {

    private CartMapper() {}

    public static CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartMapper::toItemResponse)
                .toList();

        CartSummaryResponse summary = new CartSummaryResponse(
                cart.getSubtotal().toPlainString(),
                cart.getDiscountTotal().toPlainString(),
                cart.getShippingTotal().toPlainString(),
                cart.getTaxTotal().toPlainString(),
                cart.getGrandTotal().toPlainString()
        );

        return new CartResponse(
                cart.getId().toString(),
                cart.getCurrency(),
                itemResponses,
                summary,
                cart.getTotalItemCount()
        );
    }

    private static CartItemResponse toItemResponse(CartItem item) {
        Map<String, Object> snapshot = item.getProductSnapshot();

        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                Objects.toString(snapshot.get("title"),    ""),
                Objects.toString(snapshot.get("sku"),      ""),
                Objects.toString(snapshot.get("imageUrl"), ""),
                item.getQuantity(),
                item.getUnitPrice().toPlainString(),
                item.getLineTotal().toPlainString()
        );
    }
}
