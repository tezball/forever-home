-- V1: Create app_users table with embedded notification preferences
-- This is the base identity table for all platform users
-- Named "app_users" to avoid conflicts with PostgreSQL reserved words

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    profile_complete BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Authentication tokens
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_token_expiry TIMESTAMP WITH TIME ZONE,

    -- Login attempt tracking
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,

    -- Embedded notification preferences
    notif_email_status_changes BOOLEAN NOT NULL DEFAULT TRUE,
    notif_email_new_applications BOOLEAN NOT NULL DEFAULT TRUE,
    notif_email_favorite_updates BOOLEAN NOT NULL DEFAULT TRUE,
    notif_in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- Constraints
    CONSTRAINT chk_app_users_role CHECK (role IN ('ADMIN', 'FOSTER', 'ADOPTER', 'VET', 'RESCUE_ORG')),
    CONSTRAINT chk_app_users_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'))
);

-- Indexes for common queries
CREATE INDEX idx_app_users_email ON app_users(email);
CREATE INDEX idx_app_users_role ON app_users(role);
CREATE INDEX idx_app_users_status ON app_users(status);

COMMENT ON TABLE app_users IS 'Base identity for all platform users';
COMMENT ON COLUMN app_users.role IS 'Discriminator: ADMIN, FOSTER, ADOPTER, VET, RESCUE_ORG';
COMMENT ON COLUMN app_users.status IS 'Account status: PENDING (unverified), ACTIVE, SUSPENDED';
COMMENT ON COLUMN app_users.profile_complete IS 'True after role-specific profile is filled';
