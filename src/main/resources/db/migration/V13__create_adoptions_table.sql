-- V13: Create adoptions table
-- Completed adoption records - the successful outcome

CREATE TABLE adoptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pet_id UUID NOT NULL UNIQUE REFERENCES pets(id),
    foster_id UUID NOT NULL REFERENCES fosters(id),
    adopter_id UUID NOT NULL REFERENCES adopters(id),
    rescue_org_id UUID NOT NULL REFERENCES rescue_organizations(id),
    vet_id UUID NOT NULL REFERENCES vets(id),
    application_id UUID NOT NULL REFERENCES adoption_applications(id),
    adopted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_adoptions_pet ON adoptions(pet_id);
CREATE INDEX idx_adoptions_foster ON adoptions(foster_id);
CREATE INDEX idx_adoptions_adopter ON adoptions(adopter_id);
CREATE INDEX idx_adoptions_rescue_org ON adoptions(rescue_org_id);
CREATE INDEX idx_adoptions_date ON adoptions(adopted_at);

COMMENT ON TABLE adoptions IS 'Completed adoption records capturing full chain of custody';
COMMENT ON COLUMN adoptions.adopted_at IS 'When the adoption was finalized';
