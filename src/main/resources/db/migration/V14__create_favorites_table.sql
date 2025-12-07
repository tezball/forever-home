-- V14: Create favorites table
-- Pets that adopters have saved for later

CREATE TABLE favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adopter_id UUID NOT NULL REFERENCES adopters(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_favorites_adopter_pet UNIQUE (adopter_id, pet_id)
);

-- Indexes
CREATE INDEX idx_favorites_adopter ON favorites(adopter_id);
CREATE INDEX idx_favorites_pet ON favorites(pet_id);

COMMENT ON TABLE favorites IS 'Adopter saved/favorited pets';
