package com.aiecomm.camp.modules.product.entity;

import com.aiecomm.camp.modules.inventory.entity.Inventory;
import com.aiecomm.camp.modules.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"store", "productCategory", "inventories"})
@EqualsAndHashCode(exclude = {"store", "productCategory", "inventories"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory productCategory;

    @Column(name = "sku")
    private String sku;

    @Column(name = "product_size")
    private String productSize;

    @Column(name = "product_color")
    private String productColor;

    @Column(name = "product_price")
    private Double productPrice;

    @Column(name = "in_stock")
    private Boolean inStock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Inventory> inventories = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
