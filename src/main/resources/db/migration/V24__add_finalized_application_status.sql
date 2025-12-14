-- V24: Add FINALIZED status to adoption_applications
-- This status indicates the adoption has been completed and the application is closed

-- Drop existing constraint
ALTER TABLE adoption_applications
DROP CONSTRAINT IF EXISTS chk_applications_status;

-- Add new constraint with FINALIZED status
ALTER TABLE adoption_applications
ADD CONSTRAINT chk_applications_status
CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'FINALIZED'));

-- Fix existing data: Update applications to FINALIZED where the pet is already ADOPTED
-- This corrects the data inconsistency where Oliver's application was left in APPROVED status
UPDATE adoption_applications
SET status = 'FINALIZED',
    reviewed_at = COALESCE(reviewed_at, CURRENT_TIMESTAMP)
WHERE pet_id IN (SELECT id FROM pets WHERE status = 'ADOPTED')
  AND status = 'APPROVED';

COMMENT ON COLUMN adoption_applications.status IS 'Application lifecycle: SUBMITTED -> UNDER_REVIEW -> APPROVED -> FINALIZED (or REJECTED/WITHDRAWN at any point)';
