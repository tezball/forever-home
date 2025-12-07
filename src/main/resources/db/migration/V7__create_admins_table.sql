-- V7: Create admins table
-- Minimal profile for platform administrators

CREATE TABLE admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE
);

-- Index for user lookup
CREATE INDEX idx_admins_user_id ON admins(user_id);

COMMENT ON TABLE admins IS 'Platform administrators with elevated privileges';
