-- ==========================================================
-- Flyway Migration: V6__create_media_platform_schema.sql
-- Database: PostgreSQL
-- Description: Shopify-Grade Media Platform Schema
--              (media_assets, product_images, media_references)
-- ==========================================================

-- 1. First-class media assets (independent of any consumer)
CREATE TABLE IF NOT EXISTS media_assets (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    public_url VARCHAR(1024),
    original_filename VARCHAR(255),
    content_type VARCHAR(100) NOT NULL,
    size BIGINT,
    width INTEGER,
    height INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    error_message VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ready_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_media_assets_tenant_store ON media_assets(tenant_id, store_id);
CREATE INDEX IF NOT EXISTS idx_media_assets_status ON media_assets(status);

-- 2. Product-media association (needs its own metadata: ordering, primary flag)
CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    media_asset_id BIGINT NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_images_product_asset UNIQUE (product_id, media_asset_id)
);

CREATE INDEX IF NOT EXISTS idx_product_images_product ON product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_asset ON product_images(media_asset_id);

-- Enforce at most one primary image per product
CREATE UNIQUE INDEX IF NOT EXISTS uniq_product_primary_image
    ON product_images(product_id)
    WHERE is_primary = TRUE;

-- 3. Generic reference ledger for universal reference tracking (reference-checked deletion)
CREATE TABLE IF NOT EXISTS media_references (
    id BIGSERIAL PRIMARY KEY,
    media_asset_id BIGINT NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    referenceable_type VARCHAR(50) NOT NULL,  -- 'product', 'collection', 'page', 'theme', 'blog'
    referenceable_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_media_references_asset_target UNIQUE (media_asset_id, referenceable_type, referenceable_id)
);

CREATE INDEX IF NOT EXISTS idx_media_references_asset ON media_references(media_asset_id);
CREATE INDEX IF NOT EXISTS idx_media_references_target ON media_references(referenceable_type, referenceable_id);
