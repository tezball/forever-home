-- Add moderation status tracking to pets table
-- This allows the system to block pets from becoming publicly visible
-- until AI moderation has approved their content

ALTER TABLE pets ADD COLUMN moderation_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE pets ADD COLUMN moderated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE pets ADD COLUMN moderation_notes TEXT;

-- Constraint for valid values
ALTER TABLE pets ADD CONSTRAINT chk_moderation_status
    CHECK (moderation_status IN ('PENDING', 'APPROVED', 'FLAGGED', 'REJECTED'));

-- Index for filtering by moderation status (admin review queue)
CREATE INDEX idx_pets_moderation_status ON pets(moderation_status);
