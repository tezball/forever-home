-- Allow nullable password_hash for Google-only users
ALTER TABLE app_users ALTER COLUMN password_hash DROP NOT NULL;

-- Add Google ID for OAuth linking
ALTER TABLE app_users ADD COLUMN google_id VARCHAR(255) UNIQUE;

-- Index for Google ID lookups
CREATE INDEX idx_app_users_google_id ON app_users(google_id);

-- Add comment
COMMENT ON COLUMN app_users.google_id IS 'Google OAuth subject ID for SSO users';
