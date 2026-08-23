-- Audio kitobga tavsif va (ixtiyoriy) PDF qo'shamiz
ALTER TABLE lesson_audiobooks ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE lesson_audiobooks ADD COLUMN IF NOT EXISTS pdf_path VARCHAR(500);
