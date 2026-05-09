-- ═══════════════════════════════════════════════════════
-- BASE TABLES
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(255),
    last_name       VARCHAR(255),
    email           VARCHAR(255) UNIQUE,
    phone           VARCHAR(50) UNIQUE,
    password        VARCHAR(255),
    address         VARCHAR(500),
    role            VARCHAR(50),
    ball            BIGINT DEFAULT 0,
    specialization  VARCHAR(255),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subjects (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    image_id  BIGINT
);

CREATE TABLE IF NOT EXISTS languages (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(200) NOT NULL UNIQUE,
    background_image  VARCHAR(500),
    flag_image        VARCHAR(500),
    description       VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS courses (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    image_id      BIGINT,
    subject_id    BIGINT REFERENCES subjects(id),
    language_id   BIGINT REFERENCES languages(id),
    cover_image   VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS user_courses (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    progress    DECIMAL(5,2) DEFAULT 0.00,
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, course_id)
);

CREATE TABLE IF NOT EXISTS lessons (
    id            BIGSERIAL PRIMARY KEY,
    course_id     BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    name          VARCHAR(255),
    description   TEXT,
    order_index   INT NOT NULL DEFAULT 0,
    duration_sec  INT,
    cover_image   VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS vocabulary (
    id                  BIGSERIAL PRIMARY KEY,
    lesson_id           BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    translation_uz      VARCHAR(500),
    translation_target  VARCHAR(500),
    order_index         INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tests (
    id          BIGSERIAL PRIMARY KEY,
    lesson_id   BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    name        VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS questions (
    id              BIGSERIAL PRIMARY KEY,
    test_id         BIGINT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    question_text   TEXT NOT NULL,
    option_a        VARCHAR(500),
    option_b        VARCHAR(500),
    option_c        VARCHAR(500),
    option_d        VARCHAR(500),
    correct_option  VARCHAR(10) NOT NULL,
    order_index     INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS exercises (
    id              BIGSERIAL PRIMARY KEY,
    lesson_id       BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    name            VARCHAR(255),
    order_index     INT NOT NULL DEFAULT 0,
    sentence        TEXT NOT NULL,
    options         TEXT,
    correct_answer  VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS files (
    id              BIGSERIAL PRIMARY KEY,
    path            VARCHAR(500),
    original_name   VARCHAR(500),
    mime_type       VARCHAR(100),
    size            BIGINT,
    lesson_id       BIGINT REFERENCES lessons(id) ON DELETE SET NULL,
    duration        INT,
    type            VARCHAR(50),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS news (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(500),
    image_id    BIGINT,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS attendance (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    attended_at DATE NOT NULL,
    UNIQUE (user_id, course_id, attended_at)
);

CREATE TABLE IF NOT EXISTS certificates (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    issued_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS points (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity    VARCHAR(255),
    amount      INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS teacher_questions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id   BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    question    TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS activities (
    id              BIGSERIAL PRIMARY KEY,
    activity_type   VARCHAR(50)  NOT NULL,
    measure_type    VARCHAR(50)  NOT NULL,
    value           BIGINT       NOT NULL DEFAULT 0,
    student_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_activities_student_date ON activities(student_id, created_at);

CREATE TABLE IF NOT EXISTS user_lessons (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id       BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    is_completed    BOOLEAN NOT NULL DEFAULT FALSE,
    vocab_score     INT NOT NULL DEFAULT 0,
    test_score      INT NOT NULL DEFAULT 0,
    exercise_score  INT NOT NULL DEFAULT 0,
    completed_at    TIMESTAMP,
    UNIQUE (user_id, lesson_id)
);

-- ═══════════════════════════════════════════════════════
-- SCHEMA MIGRATIONS (add new columns if not exists)
-- ═══════════════════════════════════════════════════════

ALTER TABLE languages ADD COLUMN IF NOT EXISTS color_start VARCHAR(20);
ALTER TABLE languages ADD COLUMN IF NOT EXISTS color_end   VARCHAR(20);

ALTER TABLE courses ADD COLUMN IF NOT EXISTS flag_emoji  VARCHAR(20);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS goal        VARCHAR(255);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS is_premium  BOOLEAN DEFAULT TRUE;

ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_url VARCHAR(500);

-- ═══════════════════════════════════════════════════════
-- MOCK DATA for testing lesson progression
-- ═══════════════════════════════════════════════════════

-- Test user (password = "password")
INSERT INTO users (id, first_name, last_name, email, phone, password, role, ball)
VALUES (1, 'Test', 'Student', 'test@test.com', '+998901234567', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', 100)
ON CONFLICT (id) DO NOTHING;

-- Languages
INSERT INTO languages (id, name, flag_image, background_image, description, color_start, color_end)
VALUES
(1, 'English',  'languages/English_1/English_flag_1_09994b9a.png', NULL,                                              'Ingliz tilini noldan o''rganing',   '#4FC3F7', '#1565C0'),
(2, 'Uzbek',    NULL,                                               'languages/Uzbek_2/Uzbek_background_2_d3d7d3ec.png','Ona tilni mukammal o''rganing',     '#66BB6A', '#1B5E20'),
(3, 'Russian',  NULL,                                               'languages/Russian_3/Russian_background_3_f0aff6c9.png','Rus tilini o''rganing',         '#EF5350', '#B71C1C'),
(4, 'German',   NULL,                                               NULL,                                              'Nemis tilini o''rganing',           '#F2994A', '#F2C94C'),
(5, 'Korean',   NULL,                                               NULL,                                              'Koreys tilini o''rganing',          '#A770EF', '#CF8BF3'),
(6, 'Turkish',  NULL,                                               NULL,                                              'Turk tilini o''rganing',            '#E44D26', '#F16529')
ON CONFLICT (id) DO NOTHING;

-- Courses
INSERT INTO courses (id, name, language_id, cover_image, flag_emoji, goal, is_premium)
VALUES
(1, 'English Beginner',    1, 'languages/English_1/English_flag_1_09994b9a.png', '🇬🇧', 'So''zlashuvni egallash', FALSE),
(2, 'English Intermediate',1, NULL,                                               '🇬🇧', 'Biznes ingliz tili',    TRUE),
(3, 'English Advanced',    1, NULL,                                               '🇬🇧', 'Mukammal darajaga yetish', TRUE),
(4, 'Uzbek Beginner',      2, NULL,                                               '🇺🇿', 'Ona tilni mukammal bilish', FALSE),
(5, 'Russian Beginner',    3, NULL,                                               '🇷🇺', 'Rus tilida so''zlashish', FALSE)
ON CONFLICT (id) DO NOTHING;

-- Enroll test user in English course
INSERT INTO user_courses (id, user_id, course_id, progress, created_at)
VALUES (1, 1, 1, 0.00, NOW())
ON CONFLICT (id) DO NOTHING;

-- 6 Lessons for English Beginner course (with sample test videos)
INSERT INTO lessons (id, course_id, name, description, order_index, duration_sec, video_url) VALUES
(1, 1, 'Greetings',      'Salomlashish iboralari',    1, 300, 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4'),
(2, 1, 'Numbers',        'Sonlar 1-100',               2, 360, 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4'),
(3, 1, 'Family',         'Oila a''zolari',             3, 420, 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4'),
(4, 1, 'Colors',         'Ranglar',                    4, 300, 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4'),
(5, 1, 'Food',           'Ovqatlar va ichimliklar',    5, 480, 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4'),
(6, 1, 'Daily Routine',  'Kundalik tartib',            6, 540, 'https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4')
ON CONFLICT (id) DO NOTHING;

-- Update video URLs if lessons already exist (for restart without full schema reload)
UPDATE lessons SET video_url = 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4'      WHERE id = 1 AND video_url IS NULL;
UPDATE lessons SET video_url = 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4'     WHERE id = 2 AND video_url IS NULL;
UPDATE lessons SET video_url = 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4'         WHERE id = 3 AND video_url IS NULL;
UPDATE lessons SET video_url = 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4'    WHERE id = 4 AND video_url IS NULL;
UPDATE lessons SET video_url = 'https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4'   WHERE id = 5 AND video_url IS NULL;
UPDATE lessons SET video_url = 'https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4' WHERE id = 6 AND video_url IS NULL;

-- Tests (all 6 lessons)
INSERT INTO tests (id, lesson_id, name) VALUES
(1, 1, 'Greetings Test'),
(2, 2, 'Numbers Test'),
(3, 3, 'Family Test'),
(4, 4, 'Colors Test'),
(5, 5, 'Food Test'),
(6, 6, 'Daily Routine Test')
ON CONFLICT (id) DO NOTHING;

-- Vocabulary for Lesson 1 (Greetings)
INSERT INTO vocabulary (id, lesson_id, translation_uz, translation_target, order_index) VALUES
(1, 1, 'Salom', 'Hello', 1),
(2, 1, 'Xayr', 'Goodbye', 2),
(3, 1, 'Rahmat', 'Thank you', 3),
(4, 1, 'Iltimos', 'Please', 4),
(5, 1, 'Kechirasiz', 'Excuse me', 5),
(6, 1, 'Ha', 'Yes', 6),
(7, 1, 'Yo''q', 'No', 7),
(8, 1, 'Yaxshi', 'Good', 8),
(9, 1, 'Yomon', 'Bad', 9),
(10, 1, 'Do''stim', 'My friend', 10)
ON CONFLICT (id) DO NOTHING;

-- Vocabulary for Lesson 2 (Numbers)
INSERT INTO vocabulary (id, lesson_id, translation_uz, translation_target, order_index) VALUES
(11, 2, 'Bir', 'One', 1),
(12, 2, 'Ikki', 'Two', 2),
(13, 2, 'Uch', 'Three', 3),
(14, 2, 'To''rt', 'Four', 4),
(15, 2, 'Besh', 'Five', 5),
(16, 2, 'Olti', 'Six', 6),
(17, 2, 'Yetti', 'Seven', 7),
(18, 2, 'Sakkiz', 'Eight', 8),
(19, 2, 'To''qqiz', 'Nine', 9),
(20, 2, 'O''n', 'Ten', 10)
ON CONFLICT (id) DO NOTHING;

-- Questions for Lesson 1
INSERT INTO questions (id, test_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
(1, 1, '"Salom" ingliz tilida qanday?', 'Hello', 'Goodbye', 'Thanks', 'A', 1),
(2, 1, '"Thank you" o''zbek tilida qanday?', 'Xayr', 'Rahmat', 'Salom', 'B', 2),
(3, 1, '"Goodbye" ning tarjimasi?', 'Salom', 'Iltimos', 'Xayr', 'C', 3),
(4, 1, '"Please" ning tarjimasi?', 'Iltimos', 'Rahmat', 'Kechirasiz', 'A', 4)
ON CONFLICT (id) DO NOTHING;

-- Questions for Lesson 2
INSERT INTO questions (id, test_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
(5, 2, '"Three" bu qaysi son?', 'Ikki', 'Uch', 'To''rt', 'B', 1),
(6, 2, '"Seven" ning tarjimasi?', 'Yetti', 'Sakkiz', 'Olti', 'A', 2),
(7, 2, '5 ingliz tilida qanday?', 'Four', 'Five', 'Six', 'B', 3),
(8, 2, '"O''n" ingliz tilida?', 'Nine', 'Eleven', 'Ten', 'C', 4)
ON CONFLICT (id) DO NOTHING;

-- Questions for Lesson 3 (Family)
INSERT INTO questions (id, test_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
(9,  3, '"Mother" tarjimasi?',            'Ona',     'Ota',      'Aka',      'A', 1),
(10, 3, '"Brother" tarjimasi?',           'Opa',     'Aka',      'Ota',      'B', 2),
(11, 3, '"Father" bu kim?',               'Ona',     'Ota',      'Bobo',     'B', 3),
(12, 3, '"Sister" o''zbek tilida?',       'Singilim','Akam',     'Onam',     'A', 4)
ON CONFLICT (id) DO NOTHING;

-- Questions for Lesson 4 (Colors)
INSERT INTO questions (id, test_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
(13, 4, '"Red" tarjimasi?',               'Ko''k',   'Qizil',    'Yashil',   'B', 1),
(14, 4, '"Blue" tarjimasi?',              'Ko''k',   'Sariq',    'Qora',     'A', 2),
(15, 4, '"Yellow" ingliz tilida?',        'Green',   'White',    'Yellow',   'C', 3),
(16, 4, '"Oq" ingliz tilida?',            'Black',   'White',    'Orange',   'B', 4),
(17, 4, '"Green" qaysi rang?',            'Yashil',  'Qizil',    'Ko''k',    'A', 5)
ON CONFLICT (id) DO NOTHING;

-- Questions for Lesson 5 (Food)
INSERT INTO questions (id, test_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
(18, 5, '"Non" ingliz tilida?',           'Milk',    'Bread',    'Rice',     'B', 1),
(19, 5, '"Apple" tarjimasi?',             'Banan',   'Uzum',     'Olma',     'C', 2),
(20, 5, '"Water" o''zbek tilida?',        'Sut',     'Choy',     'Suv',      'C', 3),
(21, 5, '"Egg" tarjimasi?',               'Tuxum',   'Go''sht',  'Guruch',   'A', 4),
(22, 5, '"Coffee" ingliz tilida?',        'Tea',     'Coffee',   'Juice',    'B', 5)
ON CONFLICT (id) DO NOTHING;

-- Questions for Lesson 6 (Daily Routine)
INSERT INTO questions (id, test_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
(23, 6, '"Uyg''onmoq" ingliz tilida?',   'Sleep',   'Wake up',  'Walk',     'B', 1),
(24, 6, '"Go to school" tarjimasi?',      'Uxlamoq', 'Maktabga bormoq', 'Nonushta qilmoq', 'B', 2),
(25, 6, '"Breakfast" tarjimasi?',         'Tushlik', 'Kechki ovqat', 'Nonushta', 'C', 3),
(26, 6, '"Sleep" o''zbek tilida?',        'Uxlamoq', 'Uyg''onmoq','Yurishmoq', 'A', 4),
(27, 6, '"Shower" tarjimasi?',            'Yurish',  'O''qish',  'Dush qabul qilish', 'C', 5)
ON CONFLICT (id) DO NOTHING;

-- Exercises for Lesson 1
INSERT INTO exercises (id, lesson_id, name, order_index, sentence, options, correct_answer) VALUES
(1, 1, 'Fill in the blank', 1, '___ ! How are you?', 'Hello,Goodbye,Thanks', 'Hello'),
(2, 1, 'Fill in the blank', 2, 'Thank ___ very much.', 'your,you,yours', 'you'),
(3, 1, 'Fill in the blank', 3, 'See you later. ___!', 'Hello,Please,Goodbye', 'Goodbye')
ON CONFLICT (id) DO NOTHING;

-- Exercises for Lesson 2
INSERT INTO exercises (id, lesson_id, name, order_index, sentence, options, correct_answer) VALUES
(4, 2, 'Fill in the blank', 1, '2 + 3 = ___',  'Four,Five,Six',   'Five'),
(5, 2, 'Fill in the blank', 2, '10 - 3 = ___', 'Six,Seven,Eight', 'Seven'),
(6, 2, 'Fill in the blank', 3, '4 + 4 = ___',  'Seven,Eight,Nine','Eight')
ON CONFLICT (id) DO NOTHING;

-- Exercises for Lesson 3 (Family)
INSERT INTO exercises (id, lesson_id, name, order_index, sentence, options, correct_answer) VALUES
(7,  3, 'Fill in the blank', 1, 'She is my ___.  (ona)',         'mother,father,sister', 'mother'),
(8,  3, 'Fill in the blank', 2, 'He is my ___.  (aka)',          'sister,brother,daughter', 'brother'),
(9,  3, 'Fill in the blank', 3, 'My ___ is very kind. (ota)',    'mother,uncle,father', 'father')
ON CONFLICT (id) DO NOTHING;

-- Exercises for Lesson 4 (Colors)
INSERT INTO exercises (id, lesson_id, name, order_index, sentence, options, correct_answer) VALUES
(10, 4, 'Fill in the blank', 1, 'The sky is ___.  (ko''k)',      'red,blue,green',      'blue'),
(11, 4, 'Fill in the blank', 2, 'Grass is ___.  (yashil)',       'yellow,white,green',  'green'),
(12, 4, 'Fill in the blank', 3, 'The sun is ___.  (sariq)',      'black,yellow,orange', 'yellow')
ON CONFLICT (id) DO NOTHING;

-- Exercises for Lesson 5 (Food)
INSERT INTO exercises (id, lesson_id, name, order_index, sentence, options, correct_answer) VALUES
(13, 5, 'Fill in the blank', 1, 'I drink ___ every morning. (sut)', 'water,milk,juice',  'milk'),
(14, 5, 'Fill in the blank', 2, 'She eats ___ for lunch. (guruch)', 'bread,rice,meat',   'rice'),
(15, 5, 'Fill in the blank', 3, 'He likes ___ and banana. (olma)',  'apple,egg,coffee',  'apple')
ON CONFLICT (id) DO NOTHING;

-- Exercises for Lesson 6 (Daily Routine)
INSERT INTO exercises (id, lesson_id, name, order_index, sentence, options, correct_answer) VALUES
(16, 6, 'Fill in the blank', 1, 'I ___ at 7 am every day. (uyg''onaman)', 'sleep,wake up,walk',       'wake up'),
(17, 6, 'Fill in the blank', 2, 'She ___ after dinner. (dush oladi)',      'reads,showers,walks',      'showers'),
(18, 6, 'Fill in the blank', 3, 'They ___ to school at 8 am. (boradi)',    'go,sleep,eat',             'go')
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- VOCABULARY (Lessons 3-6)
-- ═══════════════════════════════════════════════════════

-- Vocabulary for Lesson 3 (Family)
INSERT INTO vocabulary (id, lesson_id, translation_uz, translation_target, order_index) VALUES
(21, 3, 'Ona',          'Mother',       1),
(22, 3, 'Ota',          'Father',       2),
(23, 3, 'Aka / Uka',    'Brother',      3),
(24, 3, 'Opa / Singil', 'Sister',       4),
(25, 3, 'O''g''il',     'Son',          5),
(26, 3, 'Qiz',          'Daughter',     6),
(27, 3, 'Buva / Bobo',  'Grandfather',  7),
(28, 3, 'Buvi / Momo',  'Grandmother',  8),
(29, 3, 'Amaki / Tog''a','Uncle',       9),
(30, 3, 'Hola / Amma',  'Aunt',         10)
ON CONFLICT (id) DO NOTHING;

-- Vocabulary for Lesson 4 (Colors)
INSERT INTO vocabulary (id, lesson_id, translation_uz, translation_target, order_index) VALUES
(31, 4, 'Qizil',    'Red',      1),
(32, 4, 'Ko''k',    'Blue',     2),
(33, 4, 'Yashil',   'Green',    3),
(34, 4, 'Sariq',    'Yellow',   4),
(35, 4, 'Oq',       'White',    5),
(36, 4, 'Qora',     'Black',    6),
(37, 4, 'To''q sariq','Orange', 7),
(38, 4, 'Binafsha', 'Purple',   8),
(39, 4, 'Pushti',   'Pink',     9),
(40, 4, 'Jigarrang','Brown',    10)
ON CONFLICT (id) DO NOTHING;

-- Vocabulary for Lesson 5 (Food)
INSERT INTO vocabulary (id, lesson_id, translation_uz, translation_target, order_index) VALUES
(41, 5, 'Non',      'Bread',    1),
(42, 5, 'Suv',      'Water',    2),
(43, 5, 'Sut',      'Milk',     3),
(44, 5, 'Tuxum',    'Egg',      4),
(45, 5, 'Olma',     'Apple',    5),
(46, 5, 'Banan',    'Banana',   6),
(47, 5, 'Guruch',   'Rice',     7),
(48, 5, 'Go''sht',  'Meat',     8),
(49, 5, 'Choy',     'Tea',      9),
(50, 5, 'Qahva',    'Coffee',   10)
ON CONFLICT (id) DO NOTHING;

-- Vocabulary for Lesson 6 (Daily Routine)
INSERT INTO vocabulary (id, lesson_id, translation_uz, translation_target, order_index) VALUES
(51, 6, 'Uyg''onmoq',           'Wake up',          1),
(52, 6, 'Nonushta qilmoq',       'Eat breakfast',    2),
(53, 6, 'Maktabga bormoq',       'Go to school',     3),
(54, 6, 'Ishlamoq',              'Work',             4),
(55, 6, 'Uxlamoq',               'Sleep',            5),
(56, 6, 'Tushlik qilmoq',        'Have lunch',       6),
(57, 6, 'Kechki ovqat yemoq',    'Have dinner',      7),
(58, 6, 'Dush qabul qilmoq',     'Take a shower',    8),
(59, 6, 'Sayr qilmoq',           'Go for a walk',    9),
(60, 6, 'Kitob o''qimoq',        'Read a book',      10)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- USER_LESSONS: lessons 1-5 completed (lesson 6 unlocked)
-- ═══════════════════════════════════════════════════════
INSERT INTO user_lessons (id, user_id, lesson_id, is_completed, vocab_score, test_score, exercise_score, completed_at)
VALUES
(1, 1, 1, true,  10, 4, 3, NOW() - INTERVAL '5 days'),
(2, 1, 2, true,  10, 4, 3, NOW() - INTERVAL '4 days'),
(3, 1, 3, true,  10, 4, 3, NOW() - INTERVAL '3 days'),
(4, 1, 4, true,  10, 5, 3, NOW() - INTERVAL '2 days'),
(5, 1, 5, true,  10, 5, 3, NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- Attendance: 5 days in a row
INSERT INTO attendance (user_id, course_id, attended_at) VALUES
(1, 1, CURRENT_DATE - 4),
(1, 1, CURRENT_DATE - 3),
(1, 1, CURRENT_DATE - 2),
(1, 1, CURRENT_DATE - 1),
(1, 1, CURRENT_DATE)
ON CONFLICT (user_id, course_id, attended_at) DO NOTHING;

-- Points earned
INSERT INTO points (id, user_id, activity, amount) VALUES
(1,  1, 'Dars yakunlash',   5),
(2,  1, 'Dars yakunlash',   5),
(3,  1, 'Dars yakunlash',   5),
(4,  1, 'Dars yakunlash',   5),
(5,  1, 'Dars yakunlash',   5),
(6,  1, 'Test yechish',     10),
(7,  1, 'Test yechish',     10),
(8,  1, 'Test yechish',     10),
(9,  1, 'Lug''at o''rganish', 3),
(10, 1, 'Lug''at o''rganish', 3)
ON CONFLICT (id) DO NOTHING;
