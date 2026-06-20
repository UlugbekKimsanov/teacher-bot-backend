-- ═══════════════════════════════════════════════════════
-- Foydalanuvchi faolligini kuzatish (oxirgi marta ilovaga kirgan vaqti).
-- Avtomatik "bugun kirmadingiz" eslatmasi uchun ishlatiladi.
-- ═══════════════════════════════════════════════════════

ALTER TABLE users ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP;
