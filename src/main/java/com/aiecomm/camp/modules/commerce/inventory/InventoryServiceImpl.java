package com.aiecomm.camp.modules.commerce.inventory;

import com.aiecomm.camp.modules.commerce.exception.CommerceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Inventory availability implementation.
 *
 * Query strategy: SUM(quantity) across all locations that belong to the store.
 * Inventory rows join through location -> store, so we scope by storeId.
 *
 * NO SELECT FOR UPDATE here — that lock belongs in OrderServiceImpl (M2).
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable(Long productId, Long storeId, int quantity) {
        return getAvailableQuantity(productId, storeId) >= quantity;
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableQuantity(Long productId, Long storeId) {
        // Plain SUM — no lock. Inventory data may be slightly stale (acceptable for cart add).
        // The definitive lock happens at order creation time in M2.
        Long total = entityManager.createQuery(
                "SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i " +
                "WHERE i.product.productId = :productId " +
                "AND i.location.store.storeId = :storeId " +
                "AND i.quantity > 0",
                Long.class)
                .setParameter("productId", productId)
                .setParameter("storeId", storeId)
                .getSingleResult();

        return total.intValue();
    }
}
