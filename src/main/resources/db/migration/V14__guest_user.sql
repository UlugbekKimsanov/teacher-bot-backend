-- Mehmon (guest) rejimi: statistika/reytingdan istisno qilinadigan foydalanuvchi
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_guest BOOLEAN DEFAULT FALSE;

INSERT INTO users (id, first_name, last_name, email, phone, password, role, ball, is_guest, created_at)
VALUES (999, 'Mehmon', '', 'guest@oazis.local', '+998000000999', '', 'STUDENT', 0, TRUE, NOW())
ON CONFLICT (id) DO UPDATE SET is_guest = TRUE;

-- ID ketma-ketligini oldinga suramiz: yangi real foydalanuvchilar 999 bilan
-- hech qachon to'qnashmasligi uchun (sequence orqaga ham qaytmaydi).
SELECT setval(
  pg_get_serial_sequence('users', 'id'),
  GREATEST((SELECT COALESCE(MAX(id), 0) FROM users WHERE id <> 999), 999) + 1,
  false
);
