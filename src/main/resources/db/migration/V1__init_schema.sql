CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_id TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    trail_start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    trail_end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    subscription_started_at TIMESTAMP WITH TIME ZONE,
    subscription_ended_at TIMESTAMP WITH TIME ZONE,
    tokens_valid_after TIMESTAMP WITH TIME ZONE
);

CREATE TABLE blacklisted_tokens (
    id BIGSERIAL PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    email VARCHAR(255),
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_blacklisted_token ON blacklisted_tokens (token);
CREATE INDEX idx_blacklisted_expiry ON blacklisted_tokens (expiry_date);
