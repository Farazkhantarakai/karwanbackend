package com.aiecomm.camp.modules.store.entity;

import com.aiecomm.camp.modules.order.entity.Order;
import com.aiecomm.camp.modules.product.entity.Product;
import com.aiecomm.camp.modules.store.StoreEnums.StoreStatus;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private StoreStatus status;

    @Column(name = "slug",length = 100)
    String slug;

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
    @JsonManagedReference
    private List<Domain> domains = new ArrayList<>();

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Location> locations = new ArrayList<>();

    @OneToOne(mappedBy = "store", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Settings settings;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @Column(name = "Created_By",nullable = false)
    private String createdBy;

    @Column(name = "Updated_By",nullable = false)
    private String updatedBy;


    @PrePersist
    protected void onCreate() {
        this.createdOn = Instant.now();
        this.updatedOn = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = Instant.now();
    }

   public void  addDomain(Domain domain){
        if(domain!=null){
            this.domains.add(domain);
            domain.setStore(this);
        }

   }


}
