-- V4: Create adopters table
-- Profile for users who want to adopt pets

CREATE TABLE adopters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES app_users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    living_situation TEXT,
    pet_experience TEXT,

    -- Embedded address
    address_street VARCHAR(255),
    address_city VARCHAR(100),
    address_state VARCHAR(100),
    address_postal_code VARCHAR(20),
    address_country VARCHAR(100)
);

-- Index for user lookup
CREATE INDEX idx_adopters_user_id ON adopters(user_id);

COMMENT ON TABLE adopters IS 'Profile for people seeking to adopt pets';
COMMENT ON COLUMN adopters.living_situation IS 'House, apartment, etc. for rescue evaluation';
COMMENT ON COLUMN adopters.pet_experience IS 'Previous pet ownership history';
