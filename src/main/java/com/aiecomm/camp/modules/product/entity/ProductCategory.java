package com.aiecomm.camp.modules.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "products")
@EqualsAndHashCode(exclude = "products")
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @OneToMany(mappedBy = "productCategory")
    @Builder.Default
    private List<Product> products = new ArrayList<>();
}
