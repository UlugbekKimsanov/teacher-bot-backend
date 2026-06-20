-- ═══════════════════════════════════════════════════════
-- Til darajalari (daraja) — har bir til o'z darajalariga ega.
-- Kurs bitta darajaga tegishli (level_id).
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS course_levels (
    id          BIGSERIAL PRIMARY KEY,
    language_id BIGINT NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    order_index INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_course_levels_language ON course_levels(language_id);

-- Kursni darajaga bog'lash
ALTER TABLE courses ADD COLUMN IF NOT EXISTS level_id BIGINT REFERENCES course_levels(id) ON DELETE SET NULL;
