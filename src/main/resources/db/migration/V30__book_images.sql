-- Kitobga bir nechta qo'shimcha rasm (galereya) — JSON array string sifatida
ALTER TABLE books ADD COLUMN IF NOT EXISTS images TEXT;
