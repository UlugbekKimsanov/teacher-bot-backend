-- ═══════════════════════════════════════════════════════
-- SEED USERS
-- ═══════════════════════════════════════════════════════

INSERT INTO users (id, first_name, last_name, email, phone, password, role, ball)
VALUES (1, 'Test', 'Student', 'test@test.com', '+998901234567', '$2a$10$nYv8BK48mD2iAzR4X3OibeEvAG777fl46h2YU/MdvZiDO5Un4g2si', 'STUDENT', 100)
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

INSERT INTO users (id, first_name, last_name, email, phone, password, role, ball, created_at)
VALUES (100, 'Admin', 'OAZIS', 'admin@oazis.uz', '+998900000000',
        '$2a$10$nYv8BK48mD2iAzR4X3OibeEvAG777fl46h2YU/MdvZiDO5Un4g2si', 'ADMIN', 0, NOW())
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

INSERT INTO users (id, first_name, last_name, email, phone, password, role, ball, specialization, created_at)
VALUES (101, 'Nodira', 'Karimova', 'teacher@oazis.uz', '+998900000001',
        '$2a$10$nYv8BK48mD2iAzR4X3OibeEvAG777fl46h2YU/MdvZiDO5Un4g2si', 'TEACHER', 0, 'Ingliz tili', NOW())
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

-- ═══════════════════════════════════════════════════════
-- LANGUAGES
-- ═══════════════════════════════════════════════════════

INSERT INTO languages (id, name, flag_image, description, flag_emoji, color_start, color_end) VALUES
(1, 'Ingliz tili',  'languages/English_1/English_flag_1_09994b9a.png', NULL, '🇬🇧', '#4FC3F7', '#1565C0'),
(2, 'O''zbek tili', NULL, NULL, '🇺🇿', '#66BB6A', '#1B5E20'),
(3, 'Rus tili',     NULL, NULL, '🇷🇺', '#EF5350', '#6A0000'),
(4, 'Nemis tili',   NULL, NULL, '🇩🇪', '#F2994A', '#6D4C41'),
(5, 'Koreys tili',  NULL, NULL, '🇰🇷', '#A770EF', '#4A148C'),
(6, 'Turk tili',    NULL, NULL, '🇹🇷', '#E44D26', '#F16529'),
(7, 'Arab tili',    NULL, NULL, '🇸🇦', '#E44D26', '#C0392B'),
(8, 'Xitoy tili',   NULL, NULL, '🇨🇳', '#E53935', '#B71C1C'),
(9, 'Ispan tili',   NULL, NULL, '🇪🇸', '#F7B731', '#E67E22')
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- COURSES
-- ═══════════════════════════════════════════════════════

INSERT INTO courses (id, name, language_id, cover_image, flag_emoji, goal, is_premium) VALUES
(1, 'Ingliz tili — Boshlang''ich',    1, 'languages/English_1/English_flag_1_09994b9a.png', '🇬🇧', 'Noldan so''zlashuvgacha', FALSE),
(2, 'Ingliz tili — O''rta',           1, NULL, '🇬🇧', 'Biznes va kundalik hayot',  TRUE),
(3, 'Ingliz tili — Ilg''or',          1, NULL, '🇬🇧', 'C1 daraja va IELTS',        TRUE),
(4, 'O''zbek tili — Boshlang''ich',   2, NULL, '🇺🇿', 'Ona tilni mukammal bilish', FALSE),
(5, 'Rus tili — Boshlang''ich',       3, NULL, '🇷🇺', 'Alifbo va asoslar',         FALSE),
(6, 'Rus tili — O''rta',              3, NULL, '🇷🇺', 'Grammatika va so''zlashuv', TRUE),
(7, 'Turk tili — Boshlang''ich',      6, NULL, '🇹🇷', 'Asoslar va so''zlashuv',    FALSE),
(8, 'Nemis tili — Boshlang''ich',     4, NULL, '🇩🇪', 'Asoslar va grammatika',     FALSE),
(9, 'Koreys tili — Boshlang''ich',    5, NULL, '🇰🇷', 'Hangul va asoslar',         FALSE),
(10, 'Arab tili — Boshlang''ich',     7, NULL, '🇸🇦', 'Alifbo va asoslar',         FALSE),
(11, 'Xitoy tili — Boshlang''ich',    8, NULL, '🇨🇳', 'Pinyin va asoslar',         FALSE),
(12, 'Ispan tili — Boshlang''ich',    9, NULL, '🇪🇸', 'Asoslar va so''zlashuv',    FALSE)
ON CONFLICT (id) DO NOTHING;

-- Enroll test user
INSERT INTO user_courses (user_id, course_id, progress) VALUES (1, 1, 0.00), (1, 5, 0.00)
ON CONFLICT (user_id, course_id) DO NOTHING;

-- Assign teacher
INSERT INTO course_teachers (course_id, teacher_id) VALUES (1, 101), (2, 101), (3, 101)
ON CONFLICT (course_id, teacher_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- ACHIEVEMENTS
-- ═══════════════════════════════════════════════════════

INSERT INTO achievements (id, code, title, description, icon, bonus_points, condition_type, condition_value) VALUES
(1,  'first_lesson',    'Birinchi qadam',         'Birinchi darsni yakunlang',           'rocket',    5,  'lessons_completed', 1),
(2,  'five_lessons',    'Bilim izlovchi',         '5 ta darsni yakunlang',               'book',      10, 'lessons_completed', 5),
(3,  'ten_lessons',     'O''quvchi yulduzi',      '10 ta darsni yakunlang',              'star',      20, 'lessons_completed', 10),
(4,  'vocab_master_50', 'Lug''at ustasi',         '50 ta so''z o''rganing',              'dictionary',10, 'vocab_total',       50),
(5,  'vocab_master_100','So''z boyligim — 100',   '100 ta so''z o''rganing',             'crown',     20, 'vocab_total',       100),
(6,  'perfect_test',    'Mukammal test',          'Testdan 100% oling',                  'trophy',    15, 'perfect_test',      1),
(7,  'streak_3',        '3 kunlik streak',        'Ketma-ket 3 kun o''rganing',          'fire',      5,  'streak_days',       3),
(8,  'streak_7',        'Haftalik streak',        'Ketma-ket 7 kun o''rganing',          'fire',      10, 'streak_days',       7),
(9,  'streak_30',       'Oylik streak',           'Ketma-ket 30 kun o''rganing',         'fire',      30, 'streak_days',       30),
(10, 'points_50',       '50 ball',                '50 ball to''plang',                   'gem',       5,  'total_points',      50),
(11, 'points_100',      '100 ball yulduzchasi',   '100 ball to''plang',                  'gem',       10, 'total_points',      100),
(12, 'first_course',    'Kurs yakunlovchi',       'Birinchi kursni to''liq yakunlang',   'medal',     25, 'courses_completed', 1)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- SEQUENCE FIX
-- ═══════════════════════════════════════════════════════

SELECT setval('users_id_seq',       COALESCE((SELECT MAX(id) FROM users), 0));
SELECT setval('languages_id_seq',   COALESCE((SELECT MAX(id) FROM languages), 0));
SELECT setval('courses_id_seq',     COALESCE((SELECT MAX(id) FROM courses), 0));
SELECT setval('achievements_id_seq',COALESCE((SELECT MAX(id) FROM achievements), 0));
