-- ==========================================================
-- Flyway Migration: V1__init_schema.sql
-- Database: PostgreSQL
-- Description: Complete initial schema mapped to all JPA entities
-- ==========================================================

-- 1. USERS
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_id TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- 2. AUTH / BLACKLISTED TOKENS
CREATE TABLE blacklisted_tokens (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    email VARCHAR(255),
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_blacklisted_token ON blacklisted_tokens (token);
CREATE INDEX idx_blacklisted_expiry ON blacklisted_tokens (expiry_date);

-- 3. TENANTS
CREATE TABLE tenant (
    tenant_id UUID PRIMARY KEY,
    on_boarding_status VARCHAR(50) NOT NULL DEFAULT 'NotOnboarded',
    created_on TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_on TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);

-- 4. TENANT USERS (User-Tenant Membership)
CREATE TABLE tenant_users (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id) ON DELETE CASCADE,
    role VARCHAR(50),
    created_on TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_on TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_tenant_users_user ON tenant_users(user_id);
CREATE INDEX idx_tenant_users_tenant ON tenant_users(tenant_id);

-- 5. SUBSCRIPTION PLANS
CREATE TABLE subscriptionplan (
    subscription_id BIGSERIAL PRIMARY KEY,
    subscription_name VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    starting_date TIMESTAMP WITH TIME ZONE,
    ending_date TIMESTAMP WITH TIME ZONE
);

-- 6. TENANT SUBSCRIPTIONS
CREATE TABLE tenant_subscriptionplan (
    sub_id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id) ON DELETE CASCADE,
    subscription_id BIGINT NOT NULL REFERENCES subscriptionplan(subscription_id) ON DELETE CASCADE
);

-- 7. TEMPLATES
CREATE TABLE templates (
    template_id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    template_setting JSONB
);

-- 8. STORES
CREATE TABLE stores (
    store_id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(tenant_id) ON DELETE CASCADE,
    storename VARCHAR(255) NOT NULL,
    domainname VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    trail_start TIMESTAMP WITH TIME ZONE,
    trail_end TIMESTAMP WITH TIME ZONE,
    location VARCHAR(255),
    template VARCHAR(255),
    created_on TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_on TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_stores_tenant ON stores(tenant_id);

-- 9. DOMAINS
CREATE TABLE domains (
    domain_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    store_id BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    domainlink VARCHAR(255),
    created_on TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_on TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_domains_store ON domains(store_id);

-- 10. LOCATIONS
CREATE TABLE locations (
    location_id BIGSERIAL PRIMARY KEY,
    location_name VARCHAR(255) NOT NULL,
    store_id BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    location_type VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    status VARCHAR(50),
    store_type VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_locations_store ON locations(store_id);

-- 11. SETTINGS
CREATE TABLE settings (
    setting_id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL UNIQUE REFERENCES stores(store_id) ON DELETE CASCADE,
    country_code VARCHAR(50),
    country_currency VARCHAR(50),
    country_language VARCHAR(50)
);

-- 12. PRODUCT CATEGORIES
CREATE TABLE product_categories (
    category_id BIGSERIAL PRIMARY KEY,
    category_name VARCHAR(255) NOT NULL
);

-- 13. PRODUCTS
CREATE TABLE products (
    product_id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    category_id BIGINT REFERENCES product_categories(category_id) ON DELETE SET NULL,
    sku VARCHAR(255),
    product_size VARCHAR(255),
    product_color VARCHAR(255),
    product_price DOUBLE PRECISION,
    in_stock BOOLEAN DEFAULT TRUE,
    store_id BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_products_store ON products(store_id);
CREATE INDEX idx_products_category ON products(category_id);

-- 14. INVENTORY
CREATE TABLE inventory (
    inventory_id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
    location_id BIGINT NOT NULL REFERENCES locations(location_id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL DEFAULT 0,
    created_on TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_on TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_location ON inventory(location_id);

-- 15. PAYMENT METHODS
CREATE TABLE payment_methods (
    payment_method_id BIGSERIAL PRIMARY KEY,
    method_name VARCHAR(255) NOT NULL
);

-- 16. DELIVERY MODES
CREATE TABLE delivery_modes (
    delivery_id BIGSERIAL PRIMARY KEY,
    method_name VARCHAR(255) NOT NULL
);

-- 17. ORDERS
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID REFERENCES tenant(tenant_id) ON DELETE SET NULL,
    store_id BIGINT NOT NULL REFERENCES stores(store_id) ON DELETE CASCADE,
    customer_name VARCHAR(255),
    status VARCHAR(50),
    subtotal DOUBLE PRECISION,
    discount DOUBLE PRECISION,
    shipingcost DOUBLE PRECISION,
    total DOUBLE PRECISION,
    confirmation_status VARCHAR(50),
    source VARCHAR(100),
    payment_method_id BIGINT REFERENCES payment_methods(payment_method_id) ON DELETE SET NULL,
    shiping_address TEXT,
    delivery_id BIGINT REFERENCES delivery_modes(delivery_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_orders_tenant ON orders(tenant_id);
CREATE INDEX idx_orders_store ON orders(store_id);

-- 18. ORDER ITEMS
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
    productname VARCHAR(255),
    unitprice DOUBLE PRECISION,
    quantity INTEGER,
    total DOUBLE PRECISION
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- 19. CUSTOMERS
CREATE TABLE customers (
    customer_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    shipmentstatus VARCHAR(50),
    details TEXT
);

CREATE INDEX idx_customers_order ON customers(order_id);
