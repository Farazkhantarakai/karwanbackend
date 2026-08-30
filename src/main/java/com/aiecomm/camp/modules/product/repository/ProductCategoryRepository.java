package com.aiecomm.camp.modules.product.repository;

import com.aiecomm.camp.modules.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findByCategoryNameIgnoreCase(String categoryName);
}
