-- V36: Add SUPER_ADMIN role to the user roles
-- Super admins can approve rescue organizations and view platform-wide statistics

-- Drop the existing constraint
ALTER TABLE app_users DROP CONSTRAINT IF EXISTS chk_app_users_role;

-- Add new constraint with SUPER_ADMIN role
ALTER TABLE app_users ADD CONSTRAINT chk_app_users_role
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'FOSTER', 'ADOPTER', 'VET', 'RESCUE_ORG'));

COMMENT ON COLUMN app_users.role IS 'Discriminator: SUPER_ADMIN, ADMIN, FOSTER, ADOPTER, VET, RESCUE_ORG';
