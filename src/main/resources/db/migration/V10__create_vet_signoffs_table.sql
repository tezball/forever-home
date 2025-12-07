-- V10: Create vet signoffs table
-- Immutable record of veterinary verification

CREATE TABLE vet_signoffs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pet_id UUID NOT NULL UNIQUE REFERENCES pets(id),
    vet_id UUID NOT NULL REFERENCES vets(id),
    neutered_date DATE NOT NULL,
    health_status VARCHAR(50) NOT NULL,
    health_notes TEXT,
    signed_off_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_signoffs_health_status CHECK (health_status IN ('GOOD', 'KNOWN_CONDITIONS'))
);

-- Indexes
CREATE INDEX idx_signoffs_pet ON vet_signoffs(pet_id);
CREATE INDEX idx_signoffs_vet ON vet_signoffs(vet_id);
CREATE INDEX idx_signoffs_date ON vet_signoffs(signed_off_at);

COMMENT ON TABLE vet_signoffs IS 'Immutable vet verification records';
COMMENT ON COLUMN vet_signoffs.neutered_date IS 'Date pet was neutered/spayed';
COMMENT ON COLUMN vet_signoffs.health_notes IS 'Required if health_status is KNOWN_CONDITIONS';
