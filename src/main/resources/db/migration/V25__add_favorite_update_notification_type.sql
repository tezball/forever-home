-- Add FAVORITE_UPDATE to notification type constraint
-- The NotificationType enum includes FAVORITE_UPDATE but the database constraint was missing it

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS chk_notifications_type;

ALTER TABLE notifications ADD CONSTRAINT chk_notifications_type
    CHECK (type IN ('PET_STATUS_CHANGE', 'NEW_APPLICATION', 'APPLICATION_UPDATE', 'SYSTEM_ALERT', 'FAVORITE_UPDATE'));
