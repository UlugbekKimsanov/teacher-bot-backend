-- Dars videosining tejamkor (past sifatli, ~480p) varianti.
-- Server fonda ffmpeg bilan yaratadi (VideoTranscodeService); mobil ilova sekin
-- internetda shu variantni tanlashi mumkin.
--   video_url_low    — tejamkor fayl nisbiy yo'li (storage ichida), tayyor bo'lmasa NULL
--   video_low_status — NULL (hali ishlanmagan) | PENDING | PROCESSING | READY | SKIPPED | FAILED
--   video_low_error  — FAILED bo'lsa qisqa xato matni (ffmpeg stderr oxiri)
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_url_low    VARCHAR(500);
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_low_status VARCHAR(20);
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_low_error  VARCHAR(500);
