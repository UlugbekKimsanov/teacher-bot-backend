-- Kurs muallifi: admin panelda kiritiladi, dars videosida muallif sifatida ko'rsatiladi
ALTER TABLE courses ADD COLUMN IF NOT EXISTS author VARCHAR(255);
