-- Landing matnlarida imlo tuzatish: "Harid" -> "Xarid".
-- Faqat aniq iboralar almashtiriladi (havola yoki boshqa matnlarga tegmaslik uchun).
UPDATE landing_content
SET content = replace(replace(content, 'Harid qil', 'Xarid qil'), 'harid qil', 'xarid qil')
WHERE content LIKE '%arid qil%';
