-- Tillar tartibini admin drag&drop bilan boshqarish uchun order_index
ALTER TABLE languages ADD COLUMN IF NOT EXISTS order_index INT;
-- Mavjud tillarga boshlang'ich tartib (id bo'yicha)
UPDATE languages SET order_index = id WHERE order_index IS NULL;
