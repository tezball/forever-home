-- V37: Admin Panel Features
-- Adds tables for feature flags, IP blocking, platform settings, and impersonation sessions

-- Feature Flags table for runtime feature toggles
CREATE TABLE feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    rollout_percentage INTEGER DEFAULT 100,
    target_roles TEXT[],
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rollout_percentage CHECK (rollout_percentage >= 0 AND rollout_percentage <= 100)
);

CREATE INDEX idx_feature_flags_key ON feature_flags(flag_key);
CREATE INDEX idx_feature_flags_enabled ON feature_flags(enabled);

-- Insert default feature flags
INSERT INTO feature_flags (flag_key, enabled, description, rollout_percentage) VALUES
('maintenance_mode', FALSE, 'Put the site in maintenance mode', 100),
('new_user_registration', TRUE, 'Allow new user registrations', 100),
('google_oauth', TRUE, 'Enable Google OAuth login', 100),
('ai_moderation', TRUE, 'Enable AI content moderation', 100),
('email_notifications', TRUE, 'Enable email notifications', 100);

-- Blocked IPs table for abuse prevention
CREATE TABLE blocked_ips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ip_address VARCHAR(45),
    cidr_range VARCHAR(50),
    reason VARCHAR(500) NOT NULL,
    blocked_by UUID REFERENCES app_users(id),
    blocked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    permanent BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_ip_or_cidr CHECK (ip_address IS NOT NULL OR cidr_range IS NOT NULL)
);

CREATE INDEX idx_blocked_ips_ip ON blocked_ips(ip_address) WHERE active = TRUE;
CREATE INDEX idx_blocked_ips_active ON blocked_ips(active);
CREATE INDEX idx_blocked_ips_expires ON blocked_ips(expires_at) WHERE expires_at IS NOT NULL;

-- Platform Settings table for content management and configuration
CREATE TABLE platform_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    setting_type VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'general',
    description TEXT,
    updated_by UUID REFERENCES app_users(id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_setting_type CHECK (setting_type IN ('BOOLEAN', 'STRING', 'INTEGER', 'JSON', 'HTML'))
);

CREATE INDEX idx_platform_settings_key ON platform_settings(setting_key);
CREATE INDEX idx_platform_settings_category ON platform_settings(category);

-- Insert default platform settings
INSERT INTO platform_settings (setting_key, setting_value, setting_type, category, description) VALUES
('site_name', 'Forever Home', 'STRING', 'general', 'The name of the site'),
('site_tagline', 'Find your perfect companion', 'STRING', 'general', 'Site tagline shown on homepage'),
('contact_email', 'support@forever-home.ie', 'STRING', 'contact', 'Main contact email'),
('homepage_hero_title', 'Find Your Forever Friend', 'STRING', 'content', 'Homepage hero section title'),
('homepage_hero_subtitle', 'Connect with rescue organizations and adopt a pet who needs a loving home.', 'STRING', 'content', 'Homepage hero section subtitle'),
('maintenance_message', 'We are currently performing scheduled maintenance. Please check back soon.', 'STRING', 'content', 'Message shown during maintenance mode'),
('max_applications_per_user', '5', 'INTEGER', 'limits', 'Maximum active applications per adopter'),
('pet_image_max_count', '5', 'INTEGER', 'limits', 'Maximum images per pet listing');

-- Impersonation Sessions table for admin debugging
CREATE TABLE impersonation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user_id UUID NOT NULL REFERENCES app_users(id),
    target_user_id UUID NOT NULL REFERENCES app_users(id),
    token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(45),
    reason TEXT,
    CONSTRAINT chk_different_users CHECK (admin_user_id != target_user_id)
);

CREATE INDEX idx_impersonation_token ON impersonation_sessions(token) WHERE ended_at IS NULL;
CREATE INDEX idx_impersonation_admin ON impersonation_sessions(admin_user_id);
CREATE INDEX idx_impersonation_target ON impersonation_sessions(target_user_id);

-- Add rate_limit_whitelist column to app_users for manual overrides
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS rate_limit_exempt BOOLEAN NOT NULL DEFAULT FALSE;

-- Create view for active sessions (based on refresh_tokens)
CREATE OR REPLACE VIEW active_sessions AS
SELECT
    rt.id as session_id,
    rt.user_id,
    u.email,
    u.name as user_name,
    u.role,
    rt.created_at as session_started,
    rt.expires_at as session_expires,
    CASE WHEN rt.expires_at > NOW() AND NOT rt.revoked THEN TRUE ELSE FALSE END as is_active
FROM refresh_tokens rt
JOIN app_users u ON rt.user_id = u.id
WHERE rt.revoked = FALSE AND rt.expires_at > NOW();
