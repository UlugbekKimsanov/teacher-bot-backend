-- ═══════════════════════════════════════════════════════
-- Kitob: cover rasm, format/daraja olib tashlash, reyting tizimi
-- ═══════════════════════════════════════════════════════

-- Yuklangan muqova rasmi yo'li
ALTER TABLE books ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500);

-- "format" va "daraja" endi kerak emas (turi = category: digital/print)
ALTER TABLE books DROP COLUMN IF EXISTS format;
ALTER TABLE books DROP COLUMN IF EXISTS level;

-- Yangi kitob reytingi 5.0 dan boshlanadi (50 ta 5 baho asosida)
ALTER TABLE books ALTER COLUMN rating SET DEFAULT 5.0;
UPDATE books SET rating = 5.0, review_count = 0;

-- Foydalanuvchi baholari (har bir user kitobga bir marta baho qo'yadi)
CREATE TABLE IF NOT EXISTS book_ratings (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id     BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, book_id)
);
CREATE INDEX IF NOT EXISTS idx_book_ratings_book ON book_ratings(book_id);
