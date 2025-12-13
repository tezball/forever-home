-- V23: Create vet approval requests table
-- Requests from vets to be approved by rescue organizations

CREATE TABLE vet_approval_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vet_id UUID NOT NULL REFERENCES vets(id) ON DELETE CASCADE,
    rescue_org_id UUID NOT NULL REFERENCES rescue_organizations(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    message TEXT,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID REFERENCES app_users(id),
    rejection_reason TEXT,

    -- Constraints
    CONSTRAINT chk_vet_approval_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN'))
);

-- Indexes
CREATE INDEX idx_vet_approval_requests_vet_id ON vet_approval_requests(vet_id);
CREATE INDEX idx_vet_approval_requests_rescue_org_id ON vet_approval_requests(rescue_org_id);
CREATE INDEX idx_vet_approval_requests_status ON vet_approval_requests(status);

-- Index for finding pending requests for a rescue org
CREATE INDEX idx_vet_approval_requests_pending ON vet_approval_requests(rescue_org_id, status)
    WHERE status = 'PENDING';

-- Index for requested_at (useful for sorting)
CREATE INDEX idx_vet_approval_requests_requested_at ON vet_approval_requests(requested_at);

COMMENT ON TABLE vet_approval_requests IS 'Requests from vets to be approved by rescue organizations';
COMMENT ON COLUMN vet_approval_requests.vet_id IS 'The vet requesting approval';
COMMENT ON COLUMN vet_approval_requests.rescue_org_id IS 'The rescue organization being requested';
COMMENT ON COLUMN vet_approval_requests.status IS 'Current status of the request (PENDING, APPROVED, REJECTED, WITHDRAWN)';
COMMENT ON COLUMN vet_approval_requests.message IS 'Optional message from the vet explaining their request';
COMMENT ON COLUMN vet_approval_requests.requested_at IS 'When the request was submitted';
COMMENT ON COLUMN vet_approval_requests.reviewed_at IS 'When the request was reviewed';
COMMENT ON COLUMN vet_approval_requests.reviewed_by IS 'The rescue org user who reviewed the request';
COMMENT ON COLUMN vet_approval_requests.rejection_reason IS 'Optional reason for rejection';
