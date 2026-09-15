-- ==============================================================
-- Flyway Migration: V8__commerce_foundation.sql
-- Database: PostgreSQL
-- Description: Karwan Commerce Engine — Guest Cart Foundation
--
-- Design decisions:
--   - carts.tenant_id is NOT stored; derived at runtime via stores.tenant_id
--   - session_token_hash is SHA-256 of the raw token owned by Next.js
--   - cart_items has UNIQUE(cart_id, product_id) for concurrent add protection
--   - checkouts and payments tables created (schema only; services in M2)
--   - orders/order_items extended with NUMERIC columns (legacy Double kept)
-- ==============================================================

-- ────────────────────────────────────────────────────────────────
-- 1. CARTS
-- One active cart per session-token+store (partial unique index).
-- tenant_id intentionally absent — always derived from stores.tenant_id.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE carts (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id           BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    -- C1: No tenant_id column. Tenant integrity enforced through store FK.
    customer_id        BIGINT NULL,          -- reserved for M2 (StorefrontCustomer)
    session_token_hash VARCHAR(64) NULL,     -- SHA-256(rawToken); raw token lives in Next.js cookie only
    currency           VARCHAR(10)  NOT NULL DEFAULT 'PKR',
    country            VARCHAR(10)  NOT NULL DEFAULT 'PK',
    status             VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    subtotal           NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_total     NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_total          NUMERIC(19,4) NOT NULL DEFAULT 0,
    shipping_total     NUMERIC(19,4) NOT NULL DEFAULT 0,
    grand_total        NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ,
    expires_at         TIMESTAMPTZ,
    CONSTRAINT chk_cart_status CHECK (status IN ('ACTIVE','CHECKOUT','CONVERTED','ABANDONED','EXPIRED'))
);

CREATE INDEX idx_carts_store_status ON carts(store_id, status);
CREATE INDEX idx_carts_session_hash ON carts(session_token_hash, store_id)
    WHERE session_token_hash IS NOT NULL;
-- C3: Partial unique index enables ON CONFLICT re-read pattern for concurrent cart creation
CREATE UNIQUE INDEX uniq_cart_guest_active
    ON carts(session_token_hash, store_id)
    WHERE status = 'ACTIVE' AND session_token_hash IS NOT NULL;

-- ────────────────────────────────────────────────────────────────
-- 2. CART ITEMS
-- product_snapshot stores title/sku/imageUrl only — NOT price.
-- unit_price is the authoritative price stored at add-time.
-- C4: UNIQUE(cart_id, product_id) prevents concurrent duplicate inserts.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE cart_items (
    id               BIGSERIAL PRIMARY KEY,
    cart_id          UUID   NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id       BIGINT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
    -- No variant_id in V1. Added as nullable column when Variant domain arrives.
    quantity         INTEGER       NOT NULL CHECK (quantity > 0 AND quantity <= 999),
    unit_price       NUMERIC(19,4) NOT NULL CHECK (unit_price >= 0),
    line_subtotal    NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount  NUMERIC(19,4) NOT NULL DEFAULT 0,
    line_total       NUMERIC(19,4) NOT NULL DEFAULT 0,
    product_snapshot JSONB         NOT NULL DEFAULT '{}',  -- {productId, title, sku, imageUrl}
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    -- C4: Concurrent add-to-cart for same product merges into one row
    CONSTRAINT uk_cart_item_product UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart    ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product ON cart_items(product_id);

-- ────────────────────────────────────────────────────────────────
-- 3. CHECKOUTS (schema only — services built in Milestone 2)
-- Holds a pricing snapshot at checkout creation time.
-- PRICE_CHANGED detection compares snapshot vs current PricingService output.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE checkouts (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id               UUID   NOT NULL REFERENCES carts(id) ON DELETE RESTRICT,
    store_id              BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE RESTRICT,
    customer_id           BIGINT NULL,
    email                 VARCHAR(255),
    shipping_address      JSONB,
    billing_address       JSONB,
    shipping_method       VARCHAR(100),
    payment_method        VARCHAR(100),
    currency              VARCHAR(10)   NOT NULL DEFAULT 'PKR',
    subtotal              NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_total        NUMERIC(19,4) NOT NULL DEFAULT 0,
    shipping_total        NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_total             NUMERIC(19,4) NOT NULL DEFAULT 0,
    grand_total           NUMERIC(19,4) NOT NULL DEFAULT 0,
    price_snapshot_valid  BOOLEAN       NOT NULL DEFAULT TRUE,
    status                VARCHAR(30)   NOT NULL DEFAULT 'OPEN',
    idempotency_key       VARCHAR(255),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ,
    expires_at            TIMESTAMPTZ,
    CONSTRAINT uk_checkout_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_checkout_status CHECK (
        status IN ('OPEN','PENDING_PAYMENT','PAYMENT_AUTHORIZED','COMPLETED','EXPIRED','CANCELLED')
    )
);

CREATE INDEX idx_checkouts_cart  ON checkouts(cart_id);
CREATE INDEX idx_checkouts_store ON checkouts(store_id);

-- ────────────────────────────────────────────────────────────────
-- 4. PAYMENTS (schema only — state machine services built in Milestone 2)
-- Introduced now so the schema is stable when gateway integrations start.
-- ────────────────────────────────────────────────────────────────
CREATE TABLE payments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    checkout_id        UUID   NOT NULL REFERENCES checkouts(id) ON DELETE RESTRICT,
    order_id           BIGINT NULL REFERENCES orders(id) ON DELETE SET NULL,
    store_id           BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE RESTRICT,
    amount             NUMERIC(19,4) NOT NULL CHECK (amount >= 0),
    currency           VARCHAR(10)   NOT NULL DEFAULT 'PKR',
    method             VARCHAR(50)   NOT NULL,  -- COD, JAZZCASH, EASYPAISA, STRIPE, etc.
    provider           VARCHAR(50)   NOT NULL,
    status             VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    provider_reference VARCHAR(255),
    idempotency_key    VARCHAR(255)  NOT NULL,
    metadata           JSONB,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ,
    CONSTRAINT uk_payment_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payment_status CHECK (
        status IN ('PENDING','AUTHORIZED','CAPTURED','FAILED','CANCELLED','REFUNDED')
    )
);

CREATE INDEX idx_payments_checkout ON payments(checkout_id);
CREATE INDEX idx_payments_store    ON payments(store_id);
CREATE INDEX idx_payments_order    ON payments(order_id) WHERE order_id IS NOT NULL;

-- ────────────────────────────────────────────────────────────────
-- 5. EXTEND ORDERS — Add NUMERIC columns alongside legacy Double columns.
-- Legacy Double columns (subtotal, discount, shipingcost, total) are kept
-- for backward compatibility with existing merchant admin code.
-- Commerce engine writes to *_numeric columns only.
-- ────────────────────────────────────────────────────────────────
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_number              VARCHAR(50),
    ADD COLUMN IF NOT EXISTS customer_email            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS shipping_address_snapshot JSONB,
    ADD COLUMN IF NOT EXISTS billing_address_snapshot  JSONB,
    ADD COLUMN IF NOT EXISTS idempotency_key           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS subtotal_numeric          NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS discount_numeric          NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS shipping_cost_numeric     NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS total_numeric             NUMERIC(19,4);

CREATE UNIQUE INDEX IF NOT EXISTS uniq_orders_order_number
    ON orders(order_number) WHERE order_number IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uniq_orders_idempotency
    ON orders(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- ────────────────────────────────────────────────────────────────
-- 6. EXTEND ORDER ITEMS — Add NUMERIC and snapshot columns.
-- Legacy Double columns (unitprice, total) kept for compatibility.
-- ────────────────────────────────────────────────────────────────
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS unit_price_numeric NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS total_numeric      NUMERIC(19,4),
    ADD COLUMN IF NOT EXISTS product_snapshot   JSONB;
