package com.aiecomm.camp.modules.product.repository;

import com.aiecomm.camp.modules.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStore_StoreId(Long storeId);

    List<Product> findByStore_StoreIdAndInStockTrue(Long storeId);

    Optional<Product> findByProductIdAndStore_StoreId(Long productId, Long storeId);

    void deleteByProductIdAndStore_StoreId(Long productId, Long storeId);

    /**
     * Commerce engine: store-scoped active product lookup.
     * The store_id filter is in the SQL — security is enforced at the query level,
     * not as an application-level check after the fact.
     */
    @Query("SELECT p FROM Product p WHERE p.productId = :productId " +
           "AND p.store.storeId = :storeId AND p.status = 'ACTIVE'")
    Optional<Product> findActiveByProductIdAndStoreId(
            @Param("productId") Long productId,
            @Param("storeId") Long storeId);
}
