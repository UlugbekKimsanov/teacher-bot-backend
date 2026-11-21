-- ═══════════════════════════════════════════════════════
-- Admin yuborgan bildirishnomalar tarixi.
-- notifications jadvalida har qabul qiluvchi uchun alohida qator bor;
-- bu jadval esa har YUBORISH uchun bitta qator saqlaydi (kimga, nechta odamga).
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS admin_notification_log (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    body            TEXT,
    image           VARCHAR(500),
    target          VARCHAR(20),          -- ALL | STUDENTS | TEACHERS | COURSE | USERS; eski yozuvlarda NULL
    course_id       BIGINT,
    recipient_count INT NOT NULL DEFAULT 0,
    sent_by         BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admin_notification_log_created
    ON admin_notification_log(created_at DESC);

-- Avval yuborilganlarni taxminiy tiklash: bir xil sarlavha/matn/rasm bir soat ichida — bitta yuborish.
-- Kimga yuborilgani o'sha paytda saqlanmagan, shuning uchun target NULL qoladi.
INSERT INTO admin_notification_log (title, body, image, target, recipient_count, created_at)
SELECT n.title, n.body, n.image, NULL, COUNT(*), MIN(n.created_at)
FROM notifications n
WHERE n.type = 'ADMIN'
GROUP BY n.title, n.body, n.image, date_trunc('hour', n.created_at);
