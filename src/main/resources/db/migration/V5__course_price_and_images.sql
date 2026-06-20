-- ═══════════════════════════════════════════════════════
-- Kursga narx (premium uchun) + individual background rasm
-- ═══════════════════════════════════════════════════════

-- Narx (so'mda, premium kurslar uchun)
ALTER TABLE courses ADD COLUMN IF NOT EXISTS price        INTEGER;
-- Ko'rsatish uchun matn, masalan "199 000 so'm"
ALTER TABLE courses ADD COLUMN IF NOT EXISTS price_label  VARCHAR(100);
-- Kursning individual background rasmi (card ortidagi fon)
ALTER TABLE courses ADD COLUMN IF NOT EXISTS background_image VARCHAR(500);
