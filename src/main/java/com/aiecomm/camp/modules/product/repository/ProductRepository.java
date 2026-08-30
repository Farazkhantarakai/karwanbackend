package com.aiecomm.camp.modules.product.repository;

import com.aiecomm.camp.modules.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStore_StoreId(Long storeId);

    List<Product> findByStore_StoreIdAndInStockTrue(Long storeId);

    Optional<Product> findByProductIdAndStore_StoreId(Long productId, Long storeId);

    void deleteByProductIdAndStore_StoreId(Long productId, Long storeId);
}
