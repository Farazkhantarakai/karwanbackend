package com.aiecomm.camp.modules.commerce.context;

/**
 * Immutable commerce context for one storefront request.
 * Combines StoreContext (resolved from host) with the guest session token hash.
 *
 * customerId is null in Milestone 1 (no StorefrontCustomer accounts yet).
 * sessionTokenHash is SHA-256 of the raw token forwarded via X-Cart-Token header.
 */
public record CommerceContext(
        StoreContext store,
        String sessionTokenHash,   // null if no X-Cart-Token header present
        Long customerId            // null in M1; populated in M2 with StorefrontCustomer login
) {
    /** Convenience: storeId from embedded StoreContext. */
    public Long storeId() {
        return store.storeId();
    }

    /** Convenience: tenantId from embedded StoreContext. */
    public java.util.UUID tenantId() {
        return store.tenantId();
    }

    public boolean isGuest() {
        return customerId == null;
    }

    public boolean hasCartToken() {
        return sessionTokenHash != null && !sessionTokenHash.isBlank();
    }
}
