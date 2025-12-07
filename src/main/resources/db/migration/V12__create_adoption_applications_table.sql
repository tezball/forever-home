-- V12: Create adoption applications table
-- Formal requests from adopters to adopt specific pets

CREATE TABLE adoption_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pet_id UUID NOT NULL REFERENCES pets(id),
    adopter_id UUID NOT NULL REFERENCES adopters(id),
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    living_situation TEXT NOT NULL,
    pet_experience TEXT NOT NULL,
    why_adopt TEXT NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID REFERENCES users(id),
    rejection_reason TEXT,

    -- Constraints
    CONSTRAINT chk_applications_status CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT uq_applications_pet_adopter UNIQUE (pet_id, adopter_id)
);

-- Indexes
CREATE INDEX idx_applications_pet ON adoption_applications(pet_id);
CREATE INDEX idx_applications_adopter ON adoption_applications(adopter_id);
CREATE INDEX idx_applications_status ON adoption_applications(status);
CREATE INDEX idx_applications_active ON adoption_applications(pet_id, status)
    WHERE status IN ('SUBMITTED', 'UNDER_REVIEW');

COMMENT ON TABLE adoption_applications IS 'Formal adoption requests from adopters';
COMMENT ON COLUMN adoption_applications.why_adopt IS 'Motivation for wanting to adopt this pet';
COMMENT ON COLUMN adoption_applications.reviewed_by IS 'Rescue org user who reviewed the application';
