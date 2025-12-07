-- V11: Create vaccination records table
-- Individual vaccine entries within a vet signoff

CREATE TABLE vaccination_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    signoff_id UUID NOT NULL REFERENCES vet_signoffs(id) ON DELETE CASCADE,
    vaccine_name VARCHAR(100) NOT NULL,
    date_administered DATE NOT NULL,
    expiration_date DATE
);

-- Index for signoff lookup
CREATE INDEX idx_vaccinations_signoff ON vaccination_records(signoff_id);

COMMENT ON TABLE vaccination_records IS 'Vaccination entries as part of vet signoff';
COMMENT ON COLUMN vaccination_records.vaccine_name IS 'e.g., Rabies, DHPP, FVRCP';
COMMENT ON COLUMN vaccination_records.expiration_date IS 'When booster is needed';
