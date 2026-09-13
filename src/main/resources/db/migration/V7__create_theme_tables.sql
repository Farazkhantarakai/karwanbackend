-- ==========================================================
-- Flyway Migration: V7__create_theme_tables.sql
-- Database: PostgreSQL
-- Description: Theme + ThemeVersion (draft/published separation)
-- ==========================================================

CREATE TABLE IF NOT EXISTS themes (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id) ON DELETE CASCADE,
    store_id BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL DEFAULT 'Default Theme',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_themes_store UNIQUE (store_id)
);

CREATE INDEX IF NOT EXISTS idx_themes_tenant ON themes(tenant_id);

CREATE TABLE IF NOT EXISTS theme_versions (
    id BIGSERIAL PRIMARY KEY,
    theme_id BIGINT NOT NULL REFERENCES themes(id) ON DELETE CASCADE,
    version INTEGER NOT NULL DEFAULT 1,
    configuration JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_theme_versions_theme ON theme_versions(theme_id);

-- Exactly one DRAFT row and one PUBLISHED row per theme.
CREATE UNIQUE INDEX IF NOT EXISTS uniq_theme_draft ON theme_versions(theme_id) WHERE status = 'DRAFT';
CREATE UNIQUE INDEX IF NOT EXISTS uniq_theme_published ON theme_versions(theme_id) WHERE status = 'PUBLISHED';
