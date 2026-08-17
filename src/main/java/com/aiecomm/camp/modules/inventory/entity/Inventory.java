package com.aiecomm.camp.modules.inventory.entity;

import com.aiecomm.camp.modules.product.entity.Product;
import com.aiecomm.camp.modules.store.entity.Location;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"product", "location"})
@EqualsAndHashCode(exclude = {"product", "location"})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Instant createdOn;

    @Column(name = "updated_on")
    private Instant updatedOn;

    @PrePersist
    protected void onCreate() {
        this.createdOn = Instant.now();
        this.updatedOn = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = Instant.now();
    }
}
