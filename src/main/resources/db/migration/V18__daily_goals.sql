-- ═══════════════════════════════════════════════════════
-- DAILY GOALS — bugungi maqsad / kunlik daqiqa tizimi
-- ═══════════════════════════════════════════════════════

-- Foydalanuvchi maqsad sozlamalari (daqiqa / so'z)
CREATE TABLE IF NOT EXISTS user_goal_settings (
    user_id      BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    minutes_goal INT NOT NULL DEFAULT 30,
    words_goal   INT NOT NULL DEFAULT 20,
    updated_at   TIMESTAMP DEFAULT now()
);

-- Kunlik faollik (ilovada o'tkazilgan vaqt, sekundlarda)
CREATE TABLE IF NOT EXISTS daily_activity (
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL,
    seconds_spent INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, activity_date)
);

-- Kunlik maqsad tarixi (kun yakunida cron yozadi)
CREATE TABLE IF NOT EXISTS daily_goal_history (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    history_date DATE NOT NULL,
    minutes_done INT NOT NULL DEFAULT 0,
    words_done   INT NOT NULL DEFAULT 0,
    minutes_goal INT NOT NULL DEFAULT 30,
    words_goal   INT NOT NULL DEFAULT 20,
    percent      DOUBLE PRECISION NOT NULL DEFAULT 0,
    UNIQUE (user_id, history_date)
);

CREATE INDEX IF NOT EXISTS idx_daily_goal_history_user_date
    ON daily_goal_history (user_id, history_date);
