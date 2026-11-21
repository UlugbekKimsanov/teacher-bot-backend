-- break_groups V13 da aniq id (1..4) bilan seed qilingan, shuning uchun BIGSERIAL ketma-ketligi orqada qolgan:
-- admin paneldan yangi guruh qo'shishda "duplicate key" xatosi chiqardi. Ketma-ketlikni mavjud MAX(id) ga tenglaymiz.
SELECT setval(pg_get_serial_sequence('break_groups', 'id'), COALESCE((SELECT MAX(id) FROM break_groups), 1));
