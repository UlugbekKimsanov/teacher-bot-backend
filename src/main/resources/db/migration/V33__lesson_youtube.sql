-- Dars uchun YouTube unlisted video ID — pullik dars videosi YouTube orqali beriladi.
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS youtube_id VARCHAR(20);
