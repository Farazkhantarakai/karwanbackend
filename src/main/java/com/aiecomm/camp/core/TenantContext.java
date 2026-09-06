package com.aiecomm.camp.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class, prevent instantiation
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
        log.debug("TenantContext set tenantId: {}", tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
