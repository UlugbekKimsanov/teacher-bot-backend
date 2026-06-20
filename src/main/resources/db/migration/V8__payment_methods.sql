-- ═══════════════════════════════════════════════════════
-- To'lov tizimlari (payment methods) — admin enable/disable qiladi.
-- Mobil faqat enabled bo'lganlarini ko'rsatadi.
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS payment_methods (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,   -- click, payme, paynet, uzumnasiya, alifnasiya
    name        VARCHAR(100) NOT NULL,
    is_nasiya   BOOLEAN NOT NULL DEFAULT FALSE, -- nasiya (bo'lib to'lash) usulimi
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INT DEFAULT 0
);

-- Boshlang'ich: faqat Click yoqilgan, qolganlari o'chiq
INSERT INTO payment_methods (code, name, is_nasiya, enabled, order_index) VALUES
('click',      'Click',       FALSE, TRUE,  1),
('payme',      'Payme',       FALSE, FALSE, 2),
('paynet',     'Paynet',      FALSE, FALSE, 3),
('uzumnasiya', 'Uzum Nasiya', TRUE,  FALSE, 4),
('alifnasiya', 'Alif Nasiya', TRUE,  FALSE, 5)
ON CONFLICT (code) DO NOTHING;
