package com.aiecomm.camp.modules.commerce.cart.entity;

import com.aiecomm.camp.modules.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cart aggregate root.
 *
 * C1: No tenantId field stored here. Tenant integrity is guaranteed through
 * the stores FK (stores.tenant_id is the single source of truth).
 * Use cart.getTenantId() which delegates to store.getTenant().getTenantId().
 *
 * sessionTokenHash: SHA-256 of the raw token. The raw token lives only in the
 * Next.js HttpOnly cookie and the X-Cart-Token server-to-server header.
 * A DB dump exposes only the hash — not a usable token.
 */
@Entity
@Table(name = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"items", "store"})
@EqualsAndHashCode(exclude = {"items", "store"})
public class Cart {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // C1: No @Column for tenant_id — derived at runtime only
    @Column(name = "customer_id")
    private Long customerId;  // null in M1

    @Column(name = "session_token_hash", length = 64)
    private String sessionTokenHash;  // SHA-256(rawToken)

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "PKR";

    @Column(name = "country", nullable = false, length = 10)
    @Builder.Default
    private String country = "PK";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "shipping_total", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal shippingTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * C1: Derive tenant from store — never expose as a stored field.
     * Safe to call only when store is loaded (JOIN FETCH).
     */
    public UUID getTenantId() {
        return store.getTenant().getTenantId();
    }

    /** Convenience: total number of individual units in the cart. */
    public int getTotalItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
