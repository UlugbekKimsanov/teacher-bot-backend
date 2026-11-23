-- Mobil ilovaning so'nggi versiyasi (bitta qator): ilova kirishda o'zini shu bilan
-- solishtiradi va eskirgan bo'lsa ixtiyoriy yangilanish oynasini ko'rsatadi.
-- latest_build pubspec'dagi build raqami (1.0.0+15 dagi 15). Hozirgi build bilan
-- seed qilinadi — admin yangi build chiqarganda admin paneldan oshiradi.
CREATE TABLE IF NOT EXISTS app_version (
    id             SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    latest_build   INT NOT NULL,
    latest_version VARCHAR(20),
    message        TEXT,
    store_url      VARCHAR(500),
    updated_at     TIMESTAMP DEFAULT NOW()
);

INSERT INTO app_version (id, latest_build, latest_version, message, store_url)
VALUES (
    1,
    15,
    '1.0.0',
    'Ilovaning yangi versiyasi chiqdi — yangi imkoniyatlar va tuzatishlar uchun yangilang.',
    'https://play.google.com/store/apps/details?id=oazis.edu'
)
ON CONFLICT (id) DO NOTHING;
