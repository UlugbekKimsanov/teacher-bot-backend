-- Kursni saytda (landing) sotuvga chiqarish / sotuvdan olish.
--  on_sale          — kurs landing'dagi "Kurslar" bo'limida ko'rinadimi;
--  landing_students — landing kartasida ko'rsatiladigan o'quvchilar soni (admin qo'lda belgilaydi).
-- Mavjud kurslar hozir saytda ko'rinib turibdi, shuning uchun ular sotuvda qoladi (DEFAULT TRUE);
-- yangi kurslar esa admin "Sotuvga chiqarish"ni bosmaguncha sotuvda bo'lmaydi (ilova darajasida FALSE).
ALTER TABLE courses ADD COLUMN IF NOT EXISTS on_sale BOOLEAN DEFAULT TRUE;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS landing_students INTEGER DEFAULT 0;
UPDATE courses SET on_sale = TRUE WHERE on_sale IS NULL;
UPDATE courses SET landing_students = 0 WHERE landing_students IS NULL;
