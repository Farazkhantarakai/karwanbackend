package com.aiecomm.camp.modules.subscription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subscriptionplan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @Column(name = "subscription_name", nullable = false)
    private String subscriptionName;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "starting_date")
    private Instant startingDate;

    @Column(name = "ending_date")
    private Instant endingDate;

    @OneToMany(mappedBy = "subscriptionPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoreSubscriptionPlan> tenantSubscriptionPlans = new ArrayList<>();
}
