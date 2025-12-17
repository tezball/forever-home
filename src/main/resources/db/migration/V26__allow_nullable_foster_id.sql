-- V26: Allow rescue organizations to create pets directly (no foster involved)
-- Rescue-owned pets have foster_id = NULL and rescueOrgId set at creation

ALTER TABLE pets ALTER COLUMN foster_id DROP NOT NULL;

COMMENT ON COLUMN pets.foster_id IS 'Foster who registered this pet. NULL if created directly by rescue org.';
