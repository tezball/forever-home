-- V35: Add email verification token expiry
-- Security improvement: Email verification tokens should expire after 24 hours

ALTER TABLE app_users
ADD COLUMN email_verification_token_expiry TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN app_users.email_verification_token_expiry IS 'Expiry time for email verification token (24 hours from generation)';
