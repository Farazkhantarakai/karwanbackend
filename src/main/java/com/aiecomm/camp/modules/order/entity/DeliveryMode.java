package com.aiecomm.camp.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_modes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "orders")
@EqualsAndHashCode(exclude = "orders")
public class DeliveryMode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deliveryId;

    @Column(name = "method_name", nullable = false)
    private String methodName;

    @OneToMany(mappedBy = "deliveryMode")
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
}
