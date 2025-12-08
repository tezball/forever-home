-- V16: Restrict species to cats and dogs only
-- Forever Home now specializes exclusively in cat and dog adoptions

-- Update the species constraint to only allow DOG and CAT
ALTER TABLE pets DROP CONSTRAINT chk_pets_species;
ALTER TABLE pets ADD CONSTRAINT chk_pets_species CHECK (species IN ('DOG', 'CAT'));

COMMENT ON TABLE pets IS 'Cats and dogs seeking forever homes';
