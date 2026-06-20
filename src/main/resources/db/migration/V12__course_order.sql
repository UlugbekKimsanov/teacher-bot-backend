-- Kurslar tartibi (admin sozlashi mumkin). Standart = yaratilish tartibi (id).
ALTER TABLE courses ADD COLUMN IF NOT EXISTS order_index INTEGER;
UPDATE courses SET order_index = id WHERE order_index IS NULL;
