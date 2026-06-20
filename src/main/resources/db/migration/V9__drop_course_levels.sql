-- ═══════════════════════════════════════════════════════
-- Daraja (level) tushunchasi olib tashlandi.
-- Kursdan level_id ustuni va course_levels jadvali o'chiriladi.
-- ═══════════════════════════════════════════════════════

ALTER TABLE courses DROP COLUMN IF EXISTS level_id;
DROP TABLE IF EXISTS course_levels;
