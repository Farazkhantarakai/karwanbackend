package com.aiecomm.camp.modules.commerce.inventory;

/**
 * Inventory availability service for the commerce engine.
 *
 * IMPORTANT: This service NEVER locks inventory rows.
 * Locking (SELECT FOR UPDATE) happens only inside OrderServiceImpl during
 * order creation (Milestone 2). Using locks here during Add-to-Cart would
 * create unnecessary contention with 500 concurrent browsers.
 */
public interface InventoryService {

    /**
     * Check if the requested quantity is available for a product.
     * Uses a plain SUM query — no lock, safe for high-read scenarios.
     *
     * @param productId the product to check
     * @param storeId   the store context (inventory is location-scoped; locations belong to stores)
     * @param quantity  the quantity requested
     * @return true if sufficient inventory exists
     */
    boolean isAvailable(Long productId, Long storeId, int quantity);

    /**
     * Return the total available quantity across all locations for a product in a store.
     */
    int getAvailableQuantity(Long productId, Long storeId);
}
