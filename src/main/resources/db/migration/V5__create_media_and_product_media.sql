-- ==========================================================
-- Flyway Migration: V5__create_media_and_product_media.sql
-- Database: PostgreSQL
-- Description: Independent Media and ProductMedia join tables
-- ==========================================================

CREATE TABLE media (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    object_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    width INTEGER,
    height INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_media_tenant_store ON media(tenant_id, store_id);
CREATE INDEX idx_media_status_created ON media(status, created_at);

CREATE TABLE product_media (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_product_media_product_media UNIQUE (product_id, media_id)
);

CREATE INDEX idx_product_media_product ON product_media(product_id);
CREATE INDEX idx_product_media_media ON product_media(media_id);
