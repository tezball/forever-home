-- V6: Create vets table
-- Profile for veterinary professionals who verify pet health

CREATE TABLE vets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    clinic_name VARCHAR(255) NOT NULL,
    license_number VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    website VARCHAR(255),
    description TEXT,
    logo_url VARCHAR(500),
    verified BOOLEAN NOT NULL DEFAULT FALSE,

    -- Embedded address
    address_street VARCHAR(255),
    address_city VARCHAR(100),
    address_state VARCHAR(100),
    address_postal_code VARCHAR(20),
    address_country VARCHAR(100)
);

-- Indexes
CREATE INDEX idx_vets_user_id ON vets(user_id);
CREATE INDEX idx_vets_verified ON vets(verified);
CREATE INDEX idx_vets_license ON vets(license_number);

COMMENT ON TABLE vets IS 'Veterinary professionals who verify pet health before adoption';
COMMENT ON COLUMN vets.verified IS 'Admin must verify license before vet can sign off on pets';
COMMENT ON COLUMN vets.license_number IS 'Professional veterinary license ID';
