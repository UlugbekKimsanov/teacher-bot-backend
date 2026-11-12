-- Foydalanuvchi o'z akkauntini o'chira olishi uchun (Google Play talabi).
-- sale_records — buxgalteriya yozuvi: user o'chirilganda yozuv qoladi,
-- lekin foydalanuvchiga bog'lanish uziladi va xaridor ismi anonimlanadi.
ALTER TABLE sale_records ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE sale_records DROP CONSTRAINT IF EXISTS sale_records_user_id_fkey;
ALTER TABLE sale_records
    ADD CONSTRAINT sale_records_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
