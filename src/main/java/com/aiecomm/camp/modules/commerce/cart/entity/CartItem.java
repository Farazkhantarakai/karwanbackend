package com.aiecomm.camp.modules.commerce.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * A single line item in a Cart.
 *
 * productSnapshot: JSONB containing {productId, title, sku, imageUrl} ONLY.
 * Price is intentionally EXCLUDED from snapshot — unit_price is the authoritative value.
 * This separates display data (snapshot) from commerce data (unit_price).
 *
 * V1: productId only — no variantId. When the Variant domain is built, a separate
 * migration adds variant_id as a nullable column with its own FK.
 *
 * C4: UNIQUE(cart_id, product_id) is enforced at DB level — concurrent add requests
 * for the same product converge into one row via the upsert pattern in CartServiceImpl.
 */
@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cart"})
@EqualsAndHashCode(exclude = {"cart"})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private Long productId;  // No variantId in V1

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;  // Authoritative price at add-time from PricingService

    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lineSubtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    /**
     * Display-only snapshot: {productId, title, sku, imageUrl}.
     * Price is NOT in this map. See unit_price column.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_snapshot", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> productSnapshot = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
