-- ═══════════════════════════════════════════════════════
-- COURSE GROUP CHAT (kurs bo'yicha umumiy chat)
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS course_chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    sender_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_role VARCHAR(20) NOT NULL DEFAULT 'user', -- 'user' yoki 'teacher'
    sender_name VARCHAR(255),
    text        TEXT,
    media_path  VARCHAR(500),   -- yuklangan media fayl yo'li (rasm/fayl)
    media_type  VARCHAR(20),    -- 'image' yoki 'file'
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_course_chat_course_created
    ON course_chat_messages(course_id, created_at);

CREATE INDEX IF NOT EXISTS idx_course_chat_media_created
    ON course_chat_messages(created_at) WHERE media_path IS NOT NULL;

-- ═══════════════════════════════════════════════════════
-- Shaxsiy chatga ham media qo'shamiz (1 oylik tozalash bir xil bo'lishi uchun)
-- ═══════════════════════════════════════════════════════

ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS media_path VARCHAR(500);
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS media_type VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_chat_messages_media_created
    ON chat_messages(created_at) WHERE media_path IS NOT NULL;
