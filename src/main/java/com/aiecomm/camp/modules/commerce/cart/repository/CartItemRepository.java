package com.aiecomm.camp.modules.commerce.cart.repository;

import com.aiecomm.camp.modules.commerce.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * C5: Ownership-verifying lookup — loads item with its cart + store.
     * Used in update/delete operations to verify item.cart.store.storeId == ctx.storeId()
     * before executing any mutation.
     */
    @Query("SELECT ci FROM CartItem ci " +
           "JOIN FETCH ci.cart c " +
           "JOIN FETCH c.store s " +
           "WHERE ci.id = :itemId")
    Optional<CartItem> findByIdWithCartAndStore(@Param("itemId") Long itemId);

    /**
     * Find a specific item in a specific cart by productId.
     * Used for the upsert pattern (C4) when checking for existing items.
     */
    Optional<CartItem> findByCart_IdAndProductId(UUID cartId, Long productId);
}
