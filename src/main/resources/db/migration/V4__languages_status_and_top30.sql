-- ═══════════════════════════════════════════════════════
-- Tillarga enable/disable status + top 30 til
-- ═══════════════════════════════════════════════════════

-- 1) Status ustuni (default: o'chiq)
ALTER TABLE languages ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- 2) Top 30 ga to'ldirish — yangi tillar (10..30), hammasi default disabled
INSERT INTO languages (id, name, flag_emoji, color_start, color_end, enabled) VALUES
(10, 'Fransuz tili',     '🇫🇷', '#5C6BC0', '#1A237E', FALSE),
(11, 'Italyan tili',     '🇮🇹', '#388E3C', '#B71C1C', FALSE),
(12, 'Yapon tili',       '🇯🇵', '#EF5350', '#AD1457', TRUE),
(13, 'Portugal tili',    '🇵🇹', '#388E3C', '#1B5E20', FALSE),
(14, 'Hind tili',        '🇮🇳', '#FF7043', '#6D4C41', FALSE),
(15, 'Fors tili',        '🇮🇷', '#388E3C', '#006064', FALSE),
(16, 'Golland tili',     '🇳🇱', '#1565C0', '#E53935', FALSE),
(17, 'Polyak tili',      '🇵🇱', '#EF5350', '#6A0000', FALSE),
(18, 'Ukraina tili',     '🇺🇦', '#42A5F5', '#FDD835', FALSE),
(19, 'Shved tili',       '🇸🇪', '#1565C0', '#E65100', FALSE),
(20, 'Yunon tili',       '🇬🇷', '#2D9CDB', '#0B3D91', FALSE),
(21, 'Vyetnam tili',     '🇻🇳', '#EF5350', '#FDD835', FALSE),
(22, 'Tailand tili',     '🇹🇭', '#2D9CDB', '#16357A', FALSE),
(23, 'Indoneziya tili',  '🇮🇩', '#EF5350', '#B71C1C', FALSE),
(24, 'Chex tili',        '🇨🇿', '#2F80ED', '#11366B', FALSE),
(25, 'Norveg tili',      '🇳🇴', '#EF5350', '#11366B', FALSE),
(26, 'Daniya tili',      '🇩🇰', '#EF5350', '#7B1010', FALSE),
(27, 'Tojik tili',       '🇹🇯', '#26C6DA', '#006064', FALSE),
(28, 'Qozoq tili',       '🇰🇿', '#26A69A', '#004D40', FALSE),
(29, 'Ozarbayjon tili',  '🇦🇿', '#26C6DA', '#1B5E20', FALSE),
(30, 'Rumin tili',       '🇷🇴', '#2F80ED', '#F2C94C', FALSE)
ON CONFLICT (id) DO NOTHING;

-- 3) Default enabled tillar: Ingliz(1), O'zbek(2), Rus(3), Nemis(4), Arab(7), Xitoy(8), Yapon(12)
UPDATE languages SET enabled = TRUE  WHERE id IN (1, 2, 3, 4, 7, 8, 12);
-- Qolganlari aniq disabled
UPDATE languages SET enabled = FALSE WHERE id NOT IN (1, 2, 3, 4, 7, 8, 12);
