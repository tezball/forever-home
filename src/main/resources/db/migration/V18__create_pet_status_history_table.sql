-- Migration: Create pet_status_history table for tracking status changes
CREATE TABLE pet_status_history (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_by UUID REFERENCES app_users(id) ON DELETE SET NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    notes TEXT,
    CONSTRAINT chk_valid_from_status CHECK (from_status IS NULL OR from_status IN ('DRAFT', 'PENDING_RESCUE', 'PENDING_VET', 'AVAILABLE', 'IN_PROGRESS', 'ADOPTED', 'WITHDRAWN', 'ON_HOLD')),
    CONSTRAINT chk_valid_to_status CHECK (to_status IN ('DRAFT', 'PENDING_RESCUE', 'PENDING_VET', 'AVAILABLE', 'IN_PROGRESS', 'ADOPTED', 'WITHDRAWN', 'ON_HOLD'))
);

CREATE INDEX idx_pet_status_history_pet_id ON pet_status_history(pet_id);
CREATE INDEX idx_pet_status_history_changed_at ON pet_status_history(changed_at);
