-- V3: Create fosters table
-- Profile for users who register pets for adoption

CREATE TABLE fosters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50),

    -- Embedded address
    address_street VARCHAR(255),
    address_city VARCHAR(100),
    address_state VARCHAR(100),
    address_postal_code VARCHAR(20),
    address_country VARCHAR(100)
);

-- Index for user lookup
CREATE INDEX idx_fosters_user_id ON fosters(user_id);

COMMENT ON TABLE fosters IS 'Profile for pet owners seeking to rehome their pets';
COMMENT ON COLUMN fosters.address_street IS 'Optional for privacy';
