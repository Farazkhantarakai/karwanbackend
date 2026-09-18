package com.aiecomm.camp.modules.commerce.cart;

import com.aiecomm.camp.modules.commerce.cart.dto.CartResponse;
import com.aiecomm.camp.modules.commerce.cart.entity.Cart;
import com.aiecomm.camp.modules.commerce.cart.entity.CartItem;
import com.aiecomm.camp.modules.commerce.cart.entity.CartStatus;
import com.aiecomm.camp.modules.commerce.cart.repository.CartItemRepository;
import com.aiecomm.camp.modules.commerce.cart.repository.CartRepository;
import com.aiecomm.camp.modules.commerce.cart.service.CartServiceImpl;
import com.aiecomm.camp.modules.commerce.context.CommerceContext;
import com.aiecomm.camp.modules.commerce.context.StoreContext;
import com.aiecomm.camp.modules.commerce.discount.NoOpDiscountService;
import com.aiecomm.camp.modules.commerce.exception.CommerceException;
import com.aiecomm.camp.modules.commerce.inventory.InventoryService;
import com.aiecomm.camp.modules.commerce.pricing.PricingService;
import com.aiecomm.camp.modules.product.entity.Product;
import com.aiecomm.camp.modules.product.repository.ProductRepository;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private PricingService pricingService;
    @Mock private InventoryService inventoryService;
    @Spy private NoOpDiscountService discountService = new NoOpDiscountService();

    @InjectMocks
    private CartServiceImpl cartService;

    private CommerceContext context;
    private Store store;
    private Cart cart;

    @BeforeEach
    void setUp() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);

        store = new Store();
        store.setStoreId(1L);
        store.setTenant(tenant);

        StoreContext storeContext = new StoreContext(1L, tenantId, "PKR", "PK", "test-store");
        context = new CommerceContext(storeContext, "test_session_hash", null);

        cart = Cart.builder()
                .id(UUID.randomUUID())
                .store(store)
                .sessionTokenHash("test_session_hash")
                .currency("PKR")
                .country("PK")
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testGetOrCreateCart_ExistingCart() {
        when(cartRepository.findActiveBySessionTokenHashAndStoreId("test_session_hash", 1L))
                .thenReturn(Optional.of(cart));

        CartResponse response = cartService.getOrCreateCart(context);

        assertNotNull(response);
        assertEquals("PKR", response.currency());
        assertEquals(0, response.itemCount());
    }

    @Test
    void testAddItem_Success() {
        Product product = new Product();
        product.setProductId(100L);
        product.setProductName("Test Shirt");
        product.setSku("SHIRT-1");
        product.setImageUrl("https://example.com/img.jpg");

        when(cartRepository.findActiveBySessionTokenHashAndStoreId("test_session_hash", 1L))
                .thenReturn(Optional.of(cart));
        when(productRepository.findActiveByProductIdAndStoreId(100L, 1L))
                .thenReturn(Optional.of(product));
        when(inventoryService.getAvailableQuantity(100L, 1L))
                .thenReturn(10);
        when(pricingService.resolvePrice(100L, 1L))
                .thenReturn(new BigDecimal("1500.0000"));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        CartResponse response = cartService.addItem(context, 100L, 2);

        assertNotNull(response);
        assertEquals(2, response.itemCount());
        assertEquals("3000.0000", response.summary().subtotal());
        assertEquals("3000.0000", response.summary().grandTotal());
        assertEquals(1, response.items().size());
        assertEquals("Test Shirt", response.items().get(0).title());
    }

    @Test
    void testAddItem_InsufficientInventory() {
        Product product = new Product();
        product.setProductId(100L);

        when(cartRepository.findActiveBySessionTokenHashAndStoreId("test_session_hash", 1L))
                .thenReturn(Optional.of(cart));
        when(productRepository.findActiveByProductIdAndStoreId(100L, 1L))
                .thenReturn(Optional.of(product));
        when(inventoryService.getAvailableQuantity(100L, 1L))
                .thenReturn(1); // only 1 available

        CommerceException ex = assertThrows(CommerceException.class, () ->
                cartService.addItem(context, 100L, 5)
        );

        assertEquals("INSUFFICIENT_INVENTORY", ex.getCode());
    }

    @Test
    void testAddItem_InvalidQuantity() {
        CommerceException ex = assertThrows(CommerceException.class, () ->
                cartService.addItem(context, 100L, 0)
        );
        assertEquals("INVALID_QUANTITY", ex.getCode());
    }

    @Test
    void testUpdateItemQuantity_CrossStoreAccessDenied() {
        Store otherStore = new Store();
        otherStore.setStoreId(99L); // different store

        Cart otherCart = Cart.builder()
                .store(otherStore)
                .status(CartStatus.ACTIVE)
                .build();

        CartItem item = CartItem.builder()
                .id(50L)
                .cart(otherCart)
                .quantity(1)
                .build();

        when(cartItemRepository.findByIdWithCartAndStore(50L))
                .thenReturn(Optional.of(item));

        CommerceException ex = assertThrows(CommerceException.class, () ->
                cartService.updateItemQuantity(context, 50L, 3)
        );

        assertEquals("CROSS_STORE_ACCESS", ex.getCode());
    }
}
