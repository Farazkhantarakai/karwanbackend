package com.aiecomm.camp.modules.commerce.cart.service;

import com.aiecomm.camp.modules.commerce.cart.CartCalculator;
import com.aiecomm.camp.modules.commerce.cart.CartMapper;
import com.aiecomm.camp.modules.commerce.cart.ProductSnapshotService;
import com.aiecomm.camp.modules.commerce.cart.dto.CartResponse;
import com.aiecomm.camp.modules.commerce.cart.entity.Cart;
import com.aiecomm.camp.modules.commerce.cart.entity.CartItem;
import com.aiecomm.camp.modules.commerce.cart.entity.CartStatus;
import com.aiecomm.camp.modules.commerce.cart.repository.CartItemRepository;
import com.aiecomm.camp.modules.commerce.cart.repository.CartRepository;
import com.aiecomm.camp.modules.commerce.context.CommerceContext;
import com.aiecomm.camp.modules.commerce.discount.DiscountService;
import com.aiecomm.camp.modules.commerce.exception.CommerceException;
import com.aiecomm.camp.modules.commerce.inventory.InventoryService;
import com.aiecomm.camp.modules.commerce.pricing.PricingService;
import com.aiecomm.camp.modules.product.entity.Product;
import com.aiecomm.camp.modules.product.repository.ProductRepository;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * CartServiceImpl — all cart mutations in single @Transactional blocks.
 *
 * Security model:
 *  - storeId always comes from CommerceContext (derived from trusted host header)
 *  - tenantId derived from store.getTenant() — never stored redundantly (C1)
 *  - product queries are store-scoped (C4 in SD-4)
 *  - item mutations verify ownership: item -> cart -> store -> ctx.storeId() (C5)
 *
 * Concurrency:
 *  - C3: Cart creation uses try/catch DataIntegrityViolationException + re-read
 *  - C4: CartItem upsert leverages UNIQUE(cart_id, product_id) constraint
 *  - C8: No inventory lock here — only SUM check. Lock lives in OrderServiceImpl (M2).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final PricingService pricingService;
    private final InventoryService inventoryService;
    private final DiscountService discountService;

    // ── Public API ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse getOrCreateCart(CommerceContext ctx) {
        Cart cart = findOrCreateCart(ctx);
        return CartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(CommerceContext ctx, Long productId, int quantity) {
        if (quantity < 1 || quantity > 999) {
            throw new CommerceException("INVALID_QUANTITY",
                    "Quantity must be between 1 and 999");
        }

        // Step 1: Get or create cart (handles C3 concurrent creation)
        Cart cart = findOrCreateCart(ctx);

        // Step 2: Store-scoped active product lookup (C4: security at query level)
        Product product = productRepository
                .findActiveByProductIdAndStoreId(productId, ctx.storeId())
                .orElseThrow(() -> new CommerceException("PRODUCT_NOT_FOUND",
                        "Product not found or not available in this store"));

        // Step 3: Inventory check — plain SUM, no lock (C8)
        int available = inventoryService.getAvailableQuantity(productId, ctx.storeId());

        // Step 4: Resolve authoritative price (single Double->BigDecimal boundary)
        BigDecimal unitPrice = pricingService.resolvePrice(productId, ctx.storeId());

        // Step 5: Build display snapshot (no price in snapshot)
        Map<String, Object> snapshot = ProductSnapshotService.buildSnapshot(product);

        // Step 6: Upsert item (C4 — UNIQUE constraint guards concurrent inserts)
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQty = item.getQuantity() + quantity;
            if (newQty > 999) {
                throw new CommerceException("INVALID_QUANTITY",
                        "Total quantity cannot exceed 999");
            }
            // Re-check inventory for the new total quantity
            if (available < newQty) {
                throw new CommerceException("INSUFFICIENT_INVENTORY",
                        "Only " + available + " units available",
                        Map.of("availableQuantity", available));
            }
            item.setQuantity(newQty);
            item.setUnitPrice(unitPrice);  // refresh price on re-add
        } else {
            if (available < quantity) {
                throw new CommerceException("INSUFFICIENT_INVENTORY",
                        "Only " + available + " units available",
                        Map.of("availableQuantity", available));
            }
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(productId)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .productSnapshot(snapshot)
                    .build();
            cart.getItems().add(newItem);
        }

        // Step 7: Recalculate in memory — zero DB calls
        CartCalculator.recalculate(cart, discountService, ctx);

        // Step 8: Single save (cascade to items)
        Cart saved = cartRepository.save(cart);
        log.debug("Cart updated: id={} items={} total={}",
                saved.getId(), saved.getItems().size(), saved.getGrandTotal());

        return CartMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(CommerceContext ctx, Long itemId, int quantity) {
        if (quantity < 0 || quantity > 999) {
            throw new CommerceException("INVALID_QUANTITY",
                    "Quantity must be between 0 and 999 (0 removes the item)");
        }

        // C5: Ownership verification — item -> cart -> store -> ctx.storeId()
        CartItem item = findOwnedItem(itemId, ctx);

        if (quantity == 0) {
            // Treat quantity=0 as remove
            Cart cart = item.getCart();
            cart.getItems().remove(item);
            CartCalculator.recalculate(cart, discountService, ctx);
            return CartMapper.toResponse(cartRepository.save(cart));
        }

        item.setQuantity(quantity);
        Cart cart = item.getCart();
        CartCalculator.recalculate(cart, discountService, ctx);
        return CartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(CommerceContext ctx, Long itemId) {
        // C5: Ownership verification
        CartItem item = findOwnedItem(itemId, ctx);
        Cart cart = item.getCart();
        cart.getItems().remove(item);
        CartCalculator.recalculate(cart, discountService, ctx);
        return CartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse clearCart(CommerceContext ctx) {
        Cart cart = findOrCreateCart(ctx);
        cart.getItems().clear();
        CartCalculator.recalculate(cart, discountService, ctx);
        return CartMapper.toResponse(cartRepository.save(cart));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Find the active guest cart or create a new one.
     * C3: Handles concurrent cart creation with try/catch DataIntegrityViolationException.
     */
    private Cart findOrCreateCart(CommerceContext ctx) {
        if (!ctx.hasCartToken()) {
            // No token yet — create a new empty cart that will be associated
            // with a token after the Next.js Route Handler sets the cookie
            return createNewCart(ctx);
        }

        Optional<Cart> existing = cartRepository
                .findActiveBySessionTokenHashAndStoreId(ctx.sessionTokenHash(), ctx.storeId());
        if (existing.isPresent()) {
            return existing.get();
        }

        return createNewCart(ctx);
    }

    /**
     * C3: Try to create a new cart. On UNIQUE constraint violation (concurrent request
     * created the same cart first), re-read and return the existing one.
     */
    private Cart createNewCart(CommerceContext ctx) {
        Store store = storeRepository.findById(ctx.storeId())
                .orElseThrow(() -> new CommerceException("STORE_NOT_FOUND",
                        "Store not found: " + ctx.storeId()));

        Cart cart = Cart.builder()
                .id(UUID.randomUUID())
                .store(store)
                // C1: tenantId not stored — derived via cart.getTenantId() -> store.getTenant()
                .sessionTokenHash(ctx.sessionTokenHash())
                .currency(ctx.store().currency())
                .country(ctx.store().country())
                .status(CartStatus.ACTIVE)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        try {
            return cartRepository.save(cart);
        } catch (DataIntegrityViolationException e) {
            // C3: Concurrent request created the cart first — re-read it
            log.debug("Concurrent cart creation detected for store={}, re-reading", ctx.storeId());
            return cartRepository
                    .findActiveBySessionTokenHashAndStoreId(ctx.sessionTokenHash(), ctx.storeId())
                    .orElseThrow(() -> new CommerceException("CART_NOT_FOUND",
                            "Cart not found after concurrent creation attempt"));
        }
    }

    /**
     * C5: Load a CartItem and verify ownership: item -> cart -> store -> ctx.storeId().
     * Used by all mutation endpoints (update, delete) before executing any change.
     */
    private CartItem findOwnedItem(Long itemId, CommerceContext ctx) {
        CartItem item = cartItemRepository.findByIdWithCartAndStore(itemId)
                .orElseThrow(() -> new CommerceException("CART_NOT_FOUND",
                        "Cart item not found: " + itemId));

        // Verify the item's store matches the current request's store
        Long itemStoreId = item.getCart().getStore().getStoreId();
        if (!itemStoreId.equals(ctx.storeId())) {
            // Don't reveal that the item exists in another store — use generic message
            throw new CommerceException("CROSS_STORE_ACCESS",
                    "Cart item not found");
        }

        if (item.getCart().getStatus() != CartStatus.ACTIVE) {
            throw new CommerceException("CART_NOT_FOUND",
                    "Cart is no longer active");
        }

        return item;
    }
}
