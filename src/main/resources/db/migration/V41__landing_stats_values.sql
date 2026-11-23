-- Landing bosh sahifasidagi ko'rsatkichlar: o'quvchilar 1000+ -> 100+, videodarslar 280+ -> 200+.
-- Kurslar soni endi saytda sotuvdagi kurslar sonidan avtomatik olinadi (landing klientida),
-- shuning uchun u bu yerda o'zgartirilmaydi. Faqat aynan shu eski qiymatlar almashtiriladi.
UPDATE landing_content
SET content = regexp_replace(
        regexp_replace(content, '"value"\s*:\s*"1000\+"', '"value":"100+"', 'g'),
        '"value"\s*:\s*"280\+"', '"value":"200+"', 'g')
WHERE content ~ '"value"\s*:\s*"(1000|280)\+"';
