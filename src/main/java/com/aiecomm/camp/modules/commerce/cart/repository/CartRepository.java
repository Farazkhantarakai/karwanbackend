package com.aiecomm.camp.modules.commerce.cart.repository;

import com.aiecomm.camp.modules.commerce.cart.entity.Cart;
import com.aiecomm.camp.modules.commerce.cart.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    /**
     * Load active cart with all items, store, and tenant in a SINGLE query.
     * JOIN FETCH prevents N+1. Tenant is eagerly loaded so getTenantId() is safe.
     *
     * C3: UNIQUE(session_token_hash, store_id) on the DB ensures only one row
     * is returned for a given hash+store combination. The unique constraint also
     * enables the ON CONFLICT re-read pattern in CartServiceImpl.
     */
    @Query("SELECT c FROM Cart c " +
           "LEFT JOIN FETCH c.items " +
           "JOIN FETCH c.store s " +
           "JOIN FETCH s.tenant " +
           "LEFT JOIN FETCH s.settings " +
           "WHERE c.sessionTokenHash = :hash " +
           "AND s.storeId = :storeId " +
           "AND c.status = 'ACTIVE'")
    Optional<Cart> findActiveBySessionTokenHashAndStoreId(
            @Param("hash") String sessionTokenHash,
            @Param("storeId") Long storeId);

    /**
     * Load cart by UUID with items + store + tenant.
     * Used internally when we already have the cart ID.
     */
    @Query("SELECT c FROM Cart c " +
           "LEFT JOIN FETCH c.items " +
           "JOIN FETCH c.store s " +
           "JOIN FETCH s.tenant " +
           "WHERE c.id = :id")
    Optional<Cart> findByIdWithItems(@Param("id") UUID id);
}
