-- ═══════════════════════════════════════════════════════
-- Mobil → web checkout havolalari (qisqa muddatli tokenlar).
-- Mobil ilova token yaratadi, foydalanuvchi oazisedu.uz/?t=<token>
-- sahifasida to'lovni yakunlaydi. TTL — 15 daqiqa.
-- used_at — to'lov muvaffaqiyatida (webhook) belgilanadi,
-- shundan keyin token qayta ishlatilmaydi.
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS checkout_tokens (
    id           BIGSERIAL PRIMARY KEY,
    token        VARCHAR(64) NOT NULL UNIQUE,
    user_id      BIGINT NOT NULL,
    product_type VARCHAR(10) NOT NULL,          -- 'course' | 'book'
    product_id   BIGINT NOT NULL,
    created_at   TIMESTAMP DEFAULT now(),
    expires_at   TIMESTAMP NOT NULL,
    used_at      TIMESTAMP NULL
);

-- Token bo'yicha tez qidiruv (UNIQUE ham index yaratadi — bu aniq nomli qo'shimcha index)
CREATE INDEX IF NOT EXISTS idx_checkout_tokens_token
    ON checkout_tokens (token);

-- Webhook muvaffaqiyatida user+mahsulot bo'yicha faol tokenlarni bekor qilish uchun
CREATE INDEX IF NOT EXISTS idx_checkout_tokens_user_product
    ON checkout_tokens (user_id, product_type, product_id);
