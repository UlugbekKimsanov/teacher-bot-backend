-- Bosma kitoblar uchun yetkazib berish: tekin / kelishiladi / pullik (summa)
ALTER TABLE books ADD COLUMN IF NOT EXISTS delivery_type VARCHAR(20);
ALTER TABLE books ADD COLUMN IF NOT EXISTS delivery_price INTEGER;
