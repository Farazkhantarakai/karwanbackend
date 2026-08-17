package com.aiecomm.camp.modules.order.entity;

import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"store", "tenant", "paymentMethod", "deliveryMode", "orderItems", "customers"})
@EqualsAndHashCode(exclude = {"store", "tenant", "paymentMethod", "deliveryMode", "orderItems", "customers"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "status")
    private String status;

    @Column(name = "subtotal")
    private Double subtotal;

    @Column(name = "discount")
    private Double discount;

    @Column(name = "shipingcost")
    private Double shipingcost;

    @Column(name = "total")
    private Double total;

    @Column(name = "confirmation_status")
    private String confirmationStatus;

    @Column(name = "source")
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(name = "shiping_address", columnDefinition = "TEXT")
    private String shipingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private DeliveryMode deliveryMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Customer> customers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
