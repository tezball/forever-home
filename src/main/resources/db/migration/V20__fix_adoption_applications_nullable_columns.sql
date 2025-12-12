-- V20: Fix adoption applications nullable columns
-- The simple application flow only requires a message (why_adopt), not all fields
-- Make living_situation and pet_experience nullable to support the simple flow

ALTER TABLE adoption_applications
    ALTER COLUMN living_situation DROP NOT NULL,
    ALTER COLUMN pet_experience DROP NOT NULL,
    ALTER COLUMN why_adopt DROP NOT NULL;

COMMENT ON COLUMN adoption_applications.living_situation IS 'Optional: Adopter living situation details';
COMMENT ON COLUMN adoption_applications.pet_experience IS 'Optional: Prior pet experience';
COMMENT ON COLUMN adoption_applications.why_adopt IS 'Optional: Motivation for wanting to adopt this pet';
