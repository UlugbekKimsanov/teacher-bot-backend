-- Mavjud elektron kitoblarni o'z kursiga sovg'a sifatida bog'lash: kurs nomi va kitob sarlavhasi
-- aynan bir xil bo'lgan juftliklar ("Smart English" kitobi -> "Smart English" kursi).
-- Admin paneldagi "sovg'a kitob biriktirish" bilan bir xil ta'sir: bog'lanish yaratiladi va kursning
-- mavjud o'quvchilari kitobni kutubxonasiga bepul oladi (kitobi borlarga tegilmaydi).
-- Bosma kitob sovg'a qilinmaydi. Nomlari mos kelmaydigan muhitda (lokal/dev) hech narsa qilmaydi.
INSERT INTO course_gift_books (course_id, book_id)
SELECT c.id, b.id
FROM courses c
JOIN books b ON lower(btrim(b.title)) = lower(btrim(c.name))
WHERE b.category = 'digital'
ON CONFLICT (course_id, book_id) DO NOTHING;

INSERT INTO user_books (user_id, book_id, payment_method, is_active, purchased_at)
SELECT uc.user_id, b.id, 'gift', TRUE, NOW()
FROM courses c
JOIN books b ON lower(btrim(b.title)) = lower(btrim(c.name))
JOIN user_courses uc ON uc.course_id = c.id
WHERE b.category = 'digital'
ON CONFLICT (user_id, book_id) DO NOTHING;
