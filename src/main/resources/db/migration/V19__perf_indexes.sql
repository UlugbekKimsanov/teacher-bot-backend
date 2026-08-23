-- ═══════════════════════════════════════════════════════════════
-- Performance: 100K user uchun issiq jadvallarga indekslar.
-- (Production'da katta jadvallar bo'lsa CREATE INDEX CONCURRENTLY ni
--  Flyway'dan tashqari ishlatish tavsiya etiladi — bu yerda IF NOT EXISTS.)
-- ═══════════════════════════════════════════════════════════════

-- points — leaderboard SUM va "bugungi ball"
CREATE INDEX IF NOT EXISTS idx_points_user_id      ON points(user_id);
CREATE INDEX IF NOT EXISTS idx_points_user_created ON points(user_id, created_at DESC);

-- user_lessons — progress/profil JOIN va "bugun yakunlangan"
CREATE INDEX IF NOT EXISTS idx_user_lessons_lesson_id      ON user_lessons(lesson_id);
CREATE INDEX IF NOT EXISTS idx_user_lessons_user_completed ON user_lessons(user_id, completed_at);

-- notifications — har ilova ochilishida (unread, ro'yxat)
CREATE INDEX IF NOT EXISTS idx_notifications_user_read_created ON notifications(user_id, is_read, created_at DESC);

-- users — leaderboard/admin ro'yxatlari + inaktivlik cron
CREATE INDEX IF NOT EXISTS idx_users_role        ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_last_active ON users(last_active_at);

-- attendance — bugungi faollar (dashboard) + davomat oralig'i
CREATE INDEX IF NOT EXISTS idx_attendance_attended_at ON attendance(attended_at);

-- foreign key lar (JOIN/CASCADE — Postgres FK uchun avtomatik indeks yaratmaydi)
CREATE INDEX IF NOT EXISTS idx_lessons_course_id        ON lessons(course_id);
CREATE INDEX IF NOT EXISTS idx_user_courses_course_id   ON user_courses(course_id);
CREATE INDEX IF NOT EXISTS idx_vocabulary_lesson_id     ON vocabulary(lesson_id);
CREATE INDEX IF NOT EXISTS idx_exercises_lesson_id      ON exercises(lesson_id);
CREATE INDEX IF NOT EXISTS idx_tests_lesson_id          ON tests(lesson_id);
CREATE INDEX IF NOT EXISTS idx_questions_test_id        ON questions(test_id);
CREATE INDEX IF NOT EXISTS idx_files_lesson_id          ON files(lesson_id);
CREATE INDEX IF NOT EXISTS idx_certificates_user_id     ON certificates(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_teacher_questions_lesson_id ON teacher_questions(lesson_id);
CREATE INDEX IF NOT EXISTS idx_sale_records_user_id     ON sale_records(user_id);
CREATE INDEX IF NOT EXISTS idx_course_teachers_teacher_id ON course_teachers(teacher_id);

-- orders — admin daromad hisobi
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON orders(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_user_id        ON orders(user_id);

-- to'lov callback'lari (merchant_trans_id bo'yicha qidiruv)
CREATE INDEX IF NOT EXISTS idx_click_merchant_trans  ON click_transaction(merchant_trans_id);
CREATE INDEX IF NOT EXISTS idx_click_trans_id        ON click_transaction(click_trans_id);
CREATE INDEX IF NOT EXISTS idx_payme_merchant_trans  ON payme_transaction(merchant_trans_id);
CREATE INDEX IF NOT EXISTS idx_paynet_merchant_trans ON paynet_transaction(merchant_trans_id);
CREATE INDEX IF NOT EXISTS idx_uzum_merchant_trans   ON uzum_nasiya_transaction(merchant_trans_id);
CREATE INDEX IF NOT EXISTS idx_alif_merchant_trans   ON alif_transaction(merchant_trans_id);
