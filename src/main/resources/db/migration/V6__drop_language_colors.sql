-- ═══════════════════════════════════════════════════════
-- Tillardagi gradient ranglar (color_start/color_end) endi kerak emas.
-- Shuningdek eski seed'da tushmay qolgan flag_emoji'larni tiklash.
-- ═══════════════════════════════════════════════════════

-- Encoding-safe flag_emoji tiklash (chr() bilan — fayl encodingiga bog'liq emas)
UPDATE languages SET flag_emoji = chr(127480) || chr(127462) WHERE id = 7 AND flag_emoji IS NULL; -- 🇸🇦
UPDATE languages SET flag_emoji = chr(127464) || chr(127475) WHERE id = 8 AND flag_emoji IS NULL; -- 🇨🇳
UPDATE languages SET flag_emoji = chr(127466) || chr(127480) WHERE id = 9 AND flag_emoji IS NULL; -- 🇪🇸

-- Ranglar ustunlarini olib tashlash
ALTER TABLE languages DROP COLUMN IF EXISTS color_start;
ALTER TABLE languages DROP COLUMN IF EXISTS color_end;
