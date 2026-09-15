package com.aiecomm.camp.modules.commerce.cart.controller;

import com.aiecomm.camp.common.dto.ApiResponse;
import com.aiecomm.camp.modules.commerce.cart.dto.AddToCartRequest;
import com.aiecomm.camp.modules.commerce.cart.dto.CartResponse;
import com.aiecomm.camp.modules.commerce.cart.dto.UpdateCartItemRequest;
import com.aiecomm.camp.modules.commerce.cart.service.CartService;
import com.aiecomm.camp.modules.commerce.context.CommerceContext;
import com.aiecomm.camp.modules.commerce.context.CommerceContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Storefront cart REST controller.
 *
 * Security: All endpoints are under /api/storefront/** which is permitAll() in SecurityConfig.
 * The CommerceContext (set by StorefrontContextFilter) is the security boundary.
 * storeId is NEVER accepted from the client — always from CommerceContextHolder.
 *
 * Cookie management: NOT here. Next.js Route Handler owns the browser-facing cookie.
 * Spring receives the cart token via X-Cart-Token header (server-to-server only).
 *
 * POST /api/storefront/cart/items returns the complete CartResponse.
 * The frontend calls queryClient.setQueryData() directly — no second GET needed.
 */
@RestController
@RequestMapping("/api/storefront/cart")
@RequiredArgsConstructor
@Slf4j
public class StorefrontCartController {

    private final CartService cartService;

    /**
     * GET /api/storefront/cart
     * Returns the current cart. Creates an empty one if none exists.
     */
    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        CommerceContext ctx = CommerceContextHolder.get();
        CartResponse cart = cartService.getOrCreateCart(ctx);
        return ApiResponse.success(cart);
    }

    /**
     * POST /api/storefront/cart/items
     * Add a product to the cart. Returns the full updated CartResponse.
     * C15: Returns complete cart — no second GET needed by frontend.
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddToCartRequest request) {
        CommerceContext ctx = CommerceContextHolder.get();
        CartResponse cart = cartService.addItem(ctx, request.productId(), request.quantity());
        return ApiResponse.success("Item added to cart", cart);
    }

    /**
     * PATCH /api/storefront/cart/items/{itemId}
     * Update quantity of an existing item. quantity=0 removes the item.
     * C5: CartServiceImpl verifies item belongs to current store before mutating.
     */
    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        CommerceContext ctx = CommerceContextHolder.get();
        CartResponse cart = cartService.updateItemQuantity(ctx, itemId, request.quantity());
        return ApiResponse.success(cart);
    }

    /**
     * DELETE /api/storefront/cart/items/{itemId}
     * Remove a specific item from the cart.
     * C5: CartServiceImpl verifies item belongs to current store before deleting.
     */
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long itemId) {
        CommerceContext ctx = CommerceContextHolder.get();
        CartResponse cart = cartService.removeItem(ctx, itemId);
        return ApiResponse.success("Item removed", cart);
    }

    /**
     * DELETE /api/storefront/cart
     * Clear all items from the cart.
     */
    @DeleteMapping
    public ApiResponse<CartResponse> clearCart() {
        CommerceContext ctx = CommerceContextHolder.get();
        CartResponse cart = cartService.clearCart(ctx);
        return ApiResponse.success("Cart cleared", cart);
    }
}
