-- Darsga biriktiriladigan audio kitoblar (audio fayllar)
CREATE TABLE IF NOT EXISTS lesson_audiobooks (
    id          BIGSERIAL PRIMARY KEY,
    lesson_id   BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    title       VARCHAR(255),
    file_path   VARCHAR(500) NOT NULL,
    order_index INTEGER DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lesson_audiobooks_lesson ON lesson_audiobooks(lesson_id);
