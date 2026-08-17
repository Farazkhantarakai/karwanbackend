package com.aiecomm.camp.modules.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "orders")
@EqualsAndHashCode(exclude = "orders")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentMethodId;

    @Column(name = "method_name", nullable = false)
    private String methodName;

    @OneToMany(mappedBy = "paymentMethod")
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
}
