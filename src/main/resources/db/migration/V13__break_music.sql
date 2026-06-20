-- ═══════════════════════════════════════════════════════
-- Tanaffus (dam olish) musiqasi: 4 guruh + har birida musiqalar to'plami
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS break_groups (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    icon             VARCHAR(20),
    background_image VARCHAR(500),
    order_index      INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS break_tracks (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES break_groups(id) ON DELETE CASCADE,
    title       VARCHAR(150),
    file_path   VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_break_tracks_group ON break_tracks(group_id);

-- 4 ta standart guruh (fon rasmi va musiqalarni admin yuklaydi)
INSERT INTO break_groups (id, name, icon, order_index) VALUES
(1, 'Okean',             '🌊', 1),
(2, 'O''rmon',           '🌲', 2),
(3, 'Tabiat hodisalari', '🌧️', 3),
(4, 'Tabiat ohanglari',  '🍃', 4)
ON CONFLICT (id) DO NOTHING;
