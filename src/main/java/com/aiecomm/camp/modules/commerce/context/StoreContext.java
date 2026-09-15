package com.aiecomm.camp.modules.commerce.context;

import java.util.UUID;

/**
 * Immutable store context resolved from the request host (or dev slug fallback).
 * Contains store identity and localization metadata.
 */
public record StoreContext(
        Long storeId,
        UUID tenantId,
        String currency,
        String country,
        String slug
) {}
