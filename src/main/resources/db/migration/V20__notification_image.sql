-- Notification'ga optional rasm (URL/path)
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS image VARCHAR(500);
