-- V28: Add breed enum constraint
-- Restrict breed to predefined values for dogs and cats

-- Update existing display name values to enum keys
UPDATE pets SET breed = 'LABRADOR' WHERE breed = 'Labrador';
UPDATE pets SET breed = 'GOLDEN_RETRIEVER' WHERE breed = 'Golden Retriever';
UPDATE pets SET breed = 'GERMAN_SHEPHERD' WHERE breed = 'German Shepherd';
UPDATE pets SET breed = 'POODLE_MIX' WHERE breed = 'Poodle Mix';
UPDATE pets SET breed = 'MAINE_COON' WHERE breed = 'Maine Coon';
UPDATE pets SET breed = 'BRITISH_SHORTHAIR' WHERE breed = 'British Shorthair';
UPDATE pets SET breed = 'RUSSIAN_BLUE' WHERE breed = 'Russian Blue';
UPDATE pets SET breed = 'SCOTTISH_FOLD' WHERE breed = 'Scottish Fold';
-- Single-word breeds: case-insensitive update
UPDATE pets SET breed = UPPER(breed) WHERE breed IN ('Beagle', 'Boxer', 'Husky', 'Bulldog', 'Rottweiler', 'Doberman', 'Tabby', 'Siamese', 'Persian', 'Ragdoll', 'Bengal', 'Abyssinian');

-- Add CHECK constraint for valid breed values (breed is nullable)
ALTER TABLE pets ADD CONSTRAINT chk_pets_breed CHECK (
    breed IS NULL OR breed IN (
        'LABRADOR', 'GOLDEN_RETRIEVER', 'GERMAN_SHEPHERD', 'BEAGLE', 'BOXER',
        'POODLE_MIX', 'HUSKY', 'BULLDOG', 'ROTTWEILER', 'DOBERMAN',
        'TABBY', 'SIAMESE', 'MAINE_COON', 'PERSIAN', 'BRITISH_SHORTHAIR',
        'RAGDOLL', 'BENGAL', 'RUSSIAN_BLUE', 'ABYSSINIAN', 'SCOTTISH_FOLD'
    )
);

COMMENT ON COLUMN pets.breed IS 'Pet breed enum value (nullable), must match species';
