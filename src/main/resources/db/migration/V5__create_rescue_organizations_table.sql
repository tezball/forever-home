-- V5: Create rescue organizations table
-- Profile for verified rescue organizations that facilitate adoptions

CREATE TABLE rescue_organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    website VARCHAR(255),
    description TEXT,
    logo_url VARCHAR(500),
    contact_name VARCHAR(200),
    contact_email VARCHAR(255),
    verified BOOLEAN NOT NULL DEFAULT FALSE,

    -- Embedded address
    address_street VARCHAR(255),
    address_city VARCHAR(100),
    address_state VARCHAR(100),
    address_postal_code VARCHAR(20),
    address_country VARCHAR(100),

    -- Embedded social links
    social_facebook VARCHAR(255),
    social_instagram VARCHAR(255),
    social_twitter VARCHAR(255),
    social_tiktok VARCHAR(255)
);

-- Indexes
CREATE INDEX idx_rescue_orgs_user_id ON rescue_organizations(user_id);
CREATE INDEX idx_rescue_orgs_verified ON rescue_organizations(verified);
CREATE INDEX idx_rescue_orgs_name ON rescue_organizations(name);

COMMENT ON TABLE rescue_organizations IS 'Verified organizations that facilitate pet adoptions';
COMMENT ON COLUMN rescue_organizations.verified IS 'Admin must verify before org can operate';
COMMENT ON COLUMN rescue_organizations.logo_url IS 'S3 URL for organization logo';
