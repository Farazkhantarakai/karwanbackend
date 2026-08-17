package com.aiecomm.camp.modules.order.entity;

import com.aiecomm.camp.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order", "product"})
@EqualsAndHashCode(exclude = {"order", "product"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "productname")
    private String productname;

    @Column(name = "unitprice")
    private Double unitprice;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "total")
    private Double total;
}
