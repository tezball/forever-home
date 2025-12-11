-- Vet Approvals table: Records which rescue organizations have approved which vets
-- A vet must be approved by a rescue organization before they can verify pets for that org

CREATE TABLE vet_approvals (
    id UUID PRIMARY KEY,
    vet_id UUID NOT NULL REFERENCES vets(id) ON DELETE CASCADE,
    rescue_org_id UUID NOT NULL REFERENCES rescue_organizations(id) ON DELETE CASCADE,
    approved_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by UUID NOT NULL REFERENCES app_users(id),

    -- Each vet can only be approved once per rescue organization
    CONSTRAINT uq_vet_approval UNIQUE (vet_id, rescue_org_id)
);

-- Index for looking up all approvals for a vet
CREATE INDEX idx_vet_approvals_vet_id ON vet_approvals(vet_id);

-- Index for looking up all approved vets for a rescue org
CREATE INDEX idx_vet_approvals_rescue_org_id ON vet_approvals(rescue_org_id);

-- Index for approval timestamp (useful for sorting/filtering)
CREATE INDEX idx_vet_approvals_approved_at ON vet_approvals(approved_at);

COMMENT ON TABLE vet_approvals IS 'Records which rescue organizations have approved which vets to verify their pets';
COMMENT ON COLUMN vet_approvals.vet_id IS 'The vet that has been approved';
COMMENT ON COLUMN vet_approvals.rescue_org_id IS 'The rescue organization that approved the vet';
COMMENT ON COLUMN vet_approvals.approved_at IS 'When the approval was granted';
COMMENT ON COLUMN vet_approvals.approved_by IS 'The user (rescue org representative) who approved the vet';
