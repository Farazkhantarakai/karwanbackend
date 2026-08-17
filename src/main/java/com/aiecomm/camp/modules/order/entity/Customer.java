package com.aiecomm.camp.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "order")
@EqualsAndHashCode(exclude = "order")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "shipmentstatus")
    private String shipmentstatus;

    @Column(name = "details")
    private String details;
}
