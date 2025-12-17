-- Allow foster_id to be NULL in adoptions table
-- Rescue-owned pets can be adopted without a foster being involved

ALTER TABLE adoptions ALTER COLUMN foster_id DROP NOT NULL;

COMMENT ON COLUMN adoptions.foster_id IS 'Foster who registered this pet. NULL if the pet was created directly by a rescue org.';
