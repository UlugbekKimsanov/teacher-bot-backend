-- ═══════════════════════════════════════════════════════
-- LANDING MODULE
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS landing_lead (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    phone       VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_landing_lead_created_at ON landing_lead (created_at DESC);

-- Kontent bitta qatorda (id=1), TEXT ustunda JSON string ko'rinishida saqlanadi.
CREATE TABLE IF NOT EXISTS landing_content (
    id       BIGINT PRIMARY KEY,
    content  TEXT NOT NULL
);

INSERT INTO landing_content (id, content) VALUES (1, '{
  "stats": [
    { "label": "o''quvchilar", "value": "1000+" },
    { "label": "kurslar", "value": "7" },
    { "label": "videodarslar", "value": "280+" }
  ],
  "features": [
    { "title": "Malakali ustozlar tomonidan to''liq video darsliklar", "description": "" },
    { "title": "Doimiy nazorat va davomat", "description": "" },
    { "title": "Mavzuga doir barcha savollarga aniq javoblar", "description": "" },
    { "title": "Istalgan paytda va istalgan joyda o''rganish", "description": "" }
  ],
  "courseInfo": [
    { "title": "Video va Audio darsliklar orqali chet tillarini o''rgatamiz", "description": "" },
    { "title": "Interaktiv va Meta ta''lim metodikalari orqali tushuntirilgan", "description": "" },
    { "title": "AI ustozdan istalgan vaqtda mavzuga doir savollarga javob bor", "description": "" },
    { "title": "Kursni muvaffaqiyatli yakunlaganingizdan so''ng maxsus Sertifikat", "description": "" }
  ],
  "goals": [
    { "title": "Atiga 4 oy ichida chet tillarida erkin muloqot darajasi", "description": "" },
    { "title": "Aqliy zo''riqishlarga qarshi uslubda oson ta''lim berish", "description": "" },
    { "title": "Vaqtingizni maksimal darajada tejab berish", "description": "" }
  ],
  "testimonials": [],
  "courses": [
    { "flag": "🇬🇧", "title": "Ingliz tili kursi", "students": 136, "price": "799 000 so''m", "rating": 5.0 },
    { "flag": "🇷🇺", "title": "Rus tili kursi", "students": 109, "price": "799 000 so''m", "rating": 5.0 },
    { "flag": "🇰🇷", "title": "Koreys tili kursi", "students": 100, "price": "799 000 so''m", "rating": 5.0 },
    { "flag": "🇹🇷", "title": "Turk tili kursi", "students": 88, "price": "799 000 so''m", "rating": 5.0 },
    { "flag": "🇸🇦", "title": "Arab tili kursi", "students": 72, "price": "799 000 so''m", "rating": 5.0 },
    { "flag": "🇩🇪", "title": "Nemis tili kursi", "students": 50, "price": "799 000 so''m", "rating": 5.0 },
    { "flag": "🇨🇳", "title": "Xitoy tili kursi", "students": 33, "price": "799 000 so''m", "rating": 5.0 }
  ],
  "bonusText": "Harid qilgan Til kursingizning maxsus interaktiv kitoblarini bepul bonus sifatida sovg''a qilamiz!",
  "contacts": { "phone": "+998 71 200 53 53", "telegram": "", "instagram": "", "youtube": "" }
}')
ON CONFLICT (id) DO NOTHING;
