package com.aiecomm.camp.modules.store.entity;

import com.aiecomm.camp.modules.order.entity.Order;
import com.aiecomm.camp.modules.product.entity.Product;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"domains", "locations", "settings", "products", "orders"})
@EqualsAndHashCode(exclude = {"domains", "locations", "settings", "products", "orders"})
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "storename", nullable = false)
    private String storename;

    @Column(name = "domainname")
    private String domainname;

    @Column(name = "status")
    private String status;

    @Column(name = "trail_start")
    private Instant trailStart;

    @Column(name = "trail_end")
    private Instant trailEnd;

    @Column(name = "location")
    private String location;

    @Column(name = "template")
    private String template;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Instant createdOn;

    @Column(name = "updated_on")
    private Instant updatedOn;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Domain> domains = new ArrayList<>();

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Location> locations = new ArrayList<>();

    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private Settings settings;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

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
