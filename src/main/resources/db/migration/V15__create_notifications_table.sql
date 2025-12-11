-- V15: Create notifications table
-- System messages to users

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(500),
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_notifications_type CHECK (type IN ('STATUS_CHANGE', 'NEW_APPLICATION', 'APPLICATION_UPDATE', 'SYSTEM_ALERT'))
);

-- Indexes
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, read) WHERE read = FALSE;
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);

COMMENT ON TABLE notifications IS 'In-app notifications for users';
COMMENT ON COLUMN notifications.type IS 'Category: STATUS_CHANGE, NEW_APPLICATION, APPLICATION_UPDATE, SYSTEM_ALERT';
COMMENT ON COLUMN notifications.link IS 'Deep link to relevant page in the app';
