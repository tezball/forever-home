-- V8: Create pets table
-- Central entity representing animals seeking adoption

CREATE TABLE pets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    species VARCHAR(50) NOT NULL,
    breed VARCHAR(100),
    age INTEGER NOT NULL,
    age_unit VARCHAR(20) NOT NULL,
    sex VARCHAR(20) NOT NULL,
    size VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    health_notes TEXT,
    microchip_id VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    foster_id UUID NOT NULL REFERENCES fosters(id),
    rescue_org_id UUID REFERENCES rescue_organizations(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_pets_species CHECK (species IN ('DOG', 'CAT', 'RABBIT', 'BIRD', 'OTHER')),
    CONSTRAINT chk_pets_age_unit CHECK (age_unit IN ('MONTHS', 'YEARS')),
    CONSTRAINT chk_pets_sex CHECK (sex IN ('MALE', 'FEMALE')),
    CONSTRAINT chk_pets_size CHECK (size IN ('SMALL', 'MEDIUM', 'LARGE')),
    CONSTRAINT chk_pets_status CHECK (status IN ('DRAFT', 'PENDING_RESCUE', 'PENDING_VET', 'AVAILABLE', 'IN_PROGRESS', 'ADOPTED', 'WITHDRAWN', 'ON_HOLD')),
    CONSTRAINT chk_pets_age_non_negative CHECK (age >= 0)
);

-- Indexes for common queries
CREATE INDEX idx_pets_status ON pets(status);
CREATE INDEX idx_pets_foster ON pets(foster_id);
CREATE INDEX idx_pets_rescue_org ON pets(rescue_org_id);
CREATE INDEX idx_pets_microchip ON pets(microchip_id);
CREATE INDEX idx_pets_species ON pets(species);
CREATE INDEX idx_pets_available ON pets(status) WHERE status = 'AVAILABLE';

COMMENT ON TABLE pets IS 'Animals seeking forever homes';
COMMENT ON COLUMN pets.microchip_id IS 'Required and immutable - used for vet lookup';
COMMENT ON COLUMN pets.status IS 'Lifecycle state: DRAFT -> PENDING_RESCUE -> PENDING_VET -> AVAILABLE -> IN_PROGRESS -> ADOPTED';
COMMENT ON COLUMN pets.description IS 'Personality, history, needs (max 500 chars)';
