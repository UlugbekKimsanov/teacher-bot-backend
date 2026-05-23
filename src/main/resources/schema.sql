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

ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

ALTER TABLE languages ADD COLUMN IF NOT EXISTS color_start VARCHAR(20);
ALTER TABLE languages ADD COLUMN IF NOT EXISTS color_end   VARCHAR(20);

ALTER TABLE courses ADD COLUMN IF NOT EXISTS flag_emoji  VARCHAR(20);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS goal        VARCHAR(255);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS is_premium  BOOLEAN DEFAULT TRUE;

ALTER TABLE lessons ADD COLUMN IF NOT EXISTS video_url  VARCHAR(500);
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500);

-- Make score columns nullable (null = not submitted, 0 = submitted with 0)
ALTER TABLE user_lessons ALTER COLUMN vocab_score DROP NOT NULL;
ALTER TABLE user_lessons ALTER COLUMN test_score DROP NOT NULL;
ALTER TABLE user_lessons ALTER COLUMN exercise_score DROP NOT NULL;
ALTER TABLE user_lessons ALTER COLUMN vocab_score DROP DEFAULT;
ALTER TABLE user_lessons ALTER COLUMN test_score DROP DEFAULT;
ALTER TABLE user_lessons ALTER COLUMN exercise_score DROP DEFAULT;

-- ═══════════════════════════════════════════════════════
-- MOCK DATA for testing lesson progression
-- ═══════════════════════════════════════════════════════

-- Test user (password = "password")
INSERT INTO users (id, first_name, last_name, email, phone, password, role, ball)
VALUES (1, 'Test', 'Student', 'test@test.com', '+998901234567', '$2a$10$nYv8BK48mD2iAzR4X3OibeEvAG777fl46h2YU/MdvZiDO5Un4g2si', 'STUDENT', 100)
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

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
(1, 1, 'Greetings',      'Salomlashish iboralari',    1, 300, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(2, 1, 'Numbers',        'Sonlar 1-100',               2, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(3, 1, 'Family',         'Oila a''zolari',             3, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
(4, 1, 'Colors',         'Ranglar',                    4, 300, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
(5, 1, 'Food',           'Ovqatlar va ichimliklar',    5, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(6, 1, 'Daily Routine',  'Kundalik tartib',            6, 540, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4')
ON CONFLICT (id) DO NOTHING;

-- Update video/cover URLs if lessons already exist (for restart without full schema reload)
UPDATE lessons SET video_url   = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'               WHERE id = 1 AND video_url IS NULL;
UPDATE lessons SET video_url   = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'              WHERE id = 2 AND video_url IS NULL;
UPDATE lessons SET video_url   = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'                  WHERE id = 3 AND video_url IS NULL;
UPDATE lessons SET video_url   = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'             WHERE id = 4 AND video_url IS NULL;
UPDATE lessons SET video_url   = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'            WHERE id = 5 AND video_url IS NULL;
UPDATE lessons SET video_url   = 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'  WHERE id = 6 AND video_url IS NULL;

UPDATE lessons SET cover_image = 'https://picsum.photos/seed/greetings/300/200'     WHERE id = 1 AND cover_image IS NULL;
UPDATE lessons SET cover_image = 'https://picsum.photos/seed/numbers/300/200'       WHERE id = 2 AND cover_image IS NULL;
UPDATE lessons SET cover_image = 'https://picsum.photos/seed/family/300/200'        WHERE id = 3 AND cover_image IS NULL;
UPDATE lessons SET cover_image = 'https://picsum.photos/seed/colors/300/200'        WHERE id = 4 AND cover_image IS NULL;
UPDATE lessons SET cover_image = 'https://picsum.photos/seed/food/300/200'          WHERE id = 5 AND cover_image IS NULL;
UPDATE lessons SET cover_image = 'https://picsum.photos/seed/dailyroutine/300/200'  WHERE id = 6 AND cover_image IS NULL;

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

-- ═══════════════════════════════════════════════════════
-- SCHEMA FIX: scores nullable (0 ≠ "not submitted")
-- ═══════════════════════════════════════════════════════

ALTER TABLE user_lessons ALTER COLUMN vocab_score     DROP NOT NULL;
ALTER TABLE user_lessons ALTER COLUMN test_score      DROP NOT NULL;
ALTER TABLE user_lessons ALTER COLUMN exercise_score  DROP NOT NULL;

-- ═══════════════════════════════════════════════════════
-- LANGUAGE & COURSE NAME UPDATES (Uzbek display names)
-- ═══════════════════════════════════════════════════════

UPDATE languages SET name = 'Ingliz tili',  description = '450,000+ talaba', color_start = '#4FC3F7', color_end = '#1565C0' WHERE id = 1;
UPDATE languages SET name = 'O''zbek tili', description = '120,000+ talaba', color_start = '#66BB6A', color_end = '#1B5E20' WHERE id = 2;
UPDATE languages SET name = 'Rus tili',     description = '280,000+ talaba', color_start = '#EF5350', color_end = '#6A0000' WHERE id = 3;
UPDATE languages SET name = 'Nemis tili',   description = '95,000+ talaba',  color_start = '#F2994A', color_end = '#6D4C41' WHERE id = 4;
UPDATE languages SET name = 'Koreys tili',  description = '120,000+ talaba', color_start = '#A770EF', color_end = '#4A148C' WHERE id = 5;
UPDATE languages SET name = 'Turk tili',    description = '180,000+ talaba', color_start = '#E44D26', color_end = '#F16529' WHERE id = 6;

UPDATE courses SET name = 'Ingliz tili — Boshlang''ich', goal = 'Noldan so''zlashuvgacha',  is_premium = FALSE WHERE id = 1;
UPDATE courses SET name = 'Ingliz tili — O''rta',        goal = 'Biznes va kundalik hayot',  is_premium = TRUE  WHERE id = 2;
UPDATE courses SET name = 'Ingliz tili — Ilg''or',       goal = 'C1 daraja va IELTS',        is_premium = TRUE  WHERE id = 3;
UPDATE courses SET name = 'O''zbek tili — Boshlang''ich',goal = 'Ona tilni mukammal bilish', is_premium = FALSE WHERE id = 4;
UPDATE courses SET name = 'Rus tili — Boshlang''ich',    goal = 'Alifbo va asoslar',         is_premium = FALSE WHERE id = 5;

-- ═══════════════════════════════════════════════════════
-- ADD MISSING LANGUAGES
-- ═══════════════════════════════════════════════════════

INSERT INTO languages (id, name, description, color_start, color_end)
VALUES
(7, 'Arab tili',   '200,000+ talaba', '#E44D26', '#C0392B'),
(8, 'Xitoy tili',  '85,000+ talaba',  '#E53935', '#B71C1C'),
(9, 'Ispan tili',  '150,000+ talaba', '#F7B731', '#E67E22')
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- ADD MISSING COURSES
-- ═══════════════════════════════════════════════════════

INSERT INTO courses (id, name, language_id, flag_emoji, goal, is_premium)
VALUES
(6,  'Rus tili — O''rta',            3, '🇷🇺', 'Grammatika va so''zlashuv',  TRUE),
(7,  'Turk tili — Boshlang''ich',    6, '🇹🇷', 'Asoslar va so''zlashuv',     FALSE),
(8,  'Nemis tili — Boshlang''ich',   4, '🇩🇪', 'Asoslar va grammatika',      FALSE),
(9,  'Koreys tili — Boshlang''ich',  5, '🇰🇷', 'Hangul va asoslar',          FALSE),
(10, 'Arab tili — Boshlang''ich',    7, '🇸🇦', 'Alifbo va asoslar',          FALSE),
(11, 'Xitoy tili — Boshlang''ich',   8, '🇨🇳', 'Pinyin va asoslar',          FALSE),
(12, 'Ispan tili — Boshlang''ich',   9, '🇪🇸', 'Asoslar va so''zlashuv',     FALSE)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- LESSONS FOR NEW COURSES
-- ═══════════════════════════════════════════════════════

-- Russian Beginner (course_id=5)
INSERT INTO lessons (id, course_id, name, description, order_index, duration_sec, video_url) VALUES
(21, 5, 'Alifbo — 1-qism',  'Rus harflari birinchi qism',   1, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(22, 5, 'Alifbo — 2-qism',  'Rus harflari ikkinchi qism',   2, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(23, 5, 'Salomlashish',      'Rus tilida salomlashish',      3, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
(24, 5, 'Raqamlar',          'Sonlar 1-20',                  4, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
(25, 5, 'Ranglar',           'Rus tilida ranglar',           5, 300, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(26, 5, 'Oila',              'Oila a''zolari rus tilida',    6, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(27, 5, 'Taom',              'Ovqatlar rus tilida',          7, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(28, 5, 'Shahar',            'Shahar va transport',          8, 540, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4')
ON CONFLICT (id) DO NOTHING;

-- Turkish Beginner (course_id=7)
INSERT INTO lessons (id, course_id, name, description, order_index, duration_sec, video_url) VALUES
(31, 7, 'Salomlashish',    'Turk tilida salomlashish',     1, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(32, 7, 'Raqamlar',        'Sonlar 1-20',                  2, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(33, 7, 'Ranglar',         'Turk tilida ranglar',          3, 300, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
(34, 7, 'Oila',            'Oila a''zolari turk tilida',   4, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
(35, 7, 'Taom',            'Ovqatlar turk tilida',         5, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(36, 7, 'Kundalik hayot',  'Kundalik iboralar',            6, 540, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4')
ON CONFLICT (id) DO NOTHING;

-- German Beginner (course_id=8)
INSERT INTO lessons (id, course_id, name, description, order_index, duration_sec, video_url) VALUES
(41, 8, 'Salomlashish',      'Nemis tilida salomlashish',  1, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(42, 8, 'Raqamlar',          'Zahlen 1-20',                2, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(43, 8, 'Ranglar',           'Farben — ranglar',           3, 300, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
(44, 8, 'Oila',              'Familie — oila',             4, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
(45, 8, 'Taom',              'Essen — ovqatlar',           5, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(46, 8, 'Grammatika asoslari','Grundgrammatik',            6, 600, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4')
ON CONFLICT (id) DO NOTHING;

-- Korean Beginner (course_id=9)
INSERT INTO lessons (id, course_id, name, description, order_index, duration_sec, video_url) VALUES
(51, 9, 'Hangul — 1-qism',  'Koreys alifbosi birinchi qism', 1, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(52, 9, 'Hangul — 2-qism',  'Koreys alifbosi ikkinchi qism', 2, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(53, 9, 'Salomlashish',     'Koreys tilida salomlashish',    3, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
(54, 9, 'Raqamlar',         'Sonlar koreys tilida',          4, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
(55, 9, 'Oila',             'Oila koreys tilida',            5, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(56, 9, 'K-POP iboralari',  'Keng tarqalgan iboralar',      6, 540, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4')
ON CONFLICT (id) DO NOTHING;

-- Arabic Beginner (course_id=10)
INSERT INTO lessons (id, course_id, name, description, order_index, duration_sec, video_url) VALUES
(61, 10, 'Arab alifbosi — 1', 'Harflar birinchi qism',      1, 600, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(62, 10, 'Arab alifbosi — 2', 'Harflar ikkinchi qism',      2, 600, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(63, 10, 'Salomlashish',      'Arab tilida salomlashish',   3, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
(64, 10, 'Raqamlar',          'Arab raqamlari',             4, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
(65, 10, 'Oila',              'Oila arab tilida',           5, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(66, 10, 'Kundalik iboralar', 'Keng ishlatiladigan so''zlar',6, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4')
ON CONFLICT (id) DO NOTHING;

-- Chinese Beginner (course_id=11)
INSERT INTO lessons (id, course_id, name, description, order_index, duration_sec, video_url) VALUES
(71, 11, 'Pinyin — 1-qism',  'Xitoy transkripsiyasi',       1, 540, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(72, 11, 'Pinyin — 2-qism',  'Xitoy fonetikasi',            2, 540, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(73, 11, 'Salomlashish',     'Xitoy tilida salomlashish',   3, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
(74, 11, 'Raqamlar',         'Xitoy raqamlari',             4, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
(75, 11, 'Ranglar',          'Xitoy tilida ranglar',        5, 300, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(76, 11, 'Kundalik hayot',   'Keng ishlatiladigan iboralar',6, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4')
ON CONFLICT (id) DO NOTHING;

-- Spanish Beginner (course_id=12)
INSERT INTO lessons (id, course_id, name, description, order_index, duration_sec, video_url) VALUES
(81, 12, 'Salomlashish',    'Ispan tilida salomlashish',   1, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(82, 12, 'Raqamlar',        'Números 1-20',                2, 360, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
(83, 12, 'Ranglar',         'Colores — ranglar',           3, 300, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
(84, 12, 'Oila',            'Familia — oila',              4, 420, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
(85, 12, 'Taom',            'Comida — ovqatlar',           5, 480, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
(86, 12, 'Kundalik hayot',  'Vida diaria — kundalik',      6, 540, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4')
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- TESTS FOR ALL NEW LESSONS
-- ═══════════════════════════════════════════════════════

INSERT INTO tests (id, lesson_id, name) VALUES
(21, 21, 'Alifbo 1 Test'),  (22, 22, 'Alifbo 2 Test'),  (23, 23, 'Salomlashish Test'),
(24, 24, 'Raqamlar Test'),  (25, 25, 'Ranglar Test'),    (26, 26, 'Oila Test'),
(27, 27, 'Taom Test'),      (28, 28, 'Shahar Test'),
(31, 31, 'Salomlashish Test'), (32, 32, 'Raqamlar Test'), (33, 33, 'Ranglar Test'),
(34, 34, 'Oila Test'),     (35, 35, 'Taom Test'),       (36, 36, 'Kundalik Test'),
(41, 41, 'Salomlashish Test'), (42, 42, 'Zahlen Test'),   (43, 43, 'Farben Test'),
(44, 44, 'Familie Test'),  (45, 45, 'Essen Test'),      (46, 46, 'Grammatik Test'),
(51, 51, 'Hangul 1 Test'), (52, 52, 'Hangul 2 Test'),   (53, 53, 'Salomlashish Test'),
(54, 54, 'Raqamlar Test'), (55, 55, 'Oila Test'),       (56, 56, 'K-POP Test'),
(61, 61, 'Alifbo 1 Test'), (62, 62, 'Alifbo 2 Test'),   (63, 63, 'Salomlashish Test'),
(64, 64, 'Raqamlar Test'), (65, 65, 'Oila Test'),       (66, 66, 'Kundalik Test'),
(71, 71, 'Pinyin 1 Test'), (72, 72, 'Pinyin 2 Test'),   (73, 73, 'Salomlashish Test'),
(74, 74, 'Raqamlar Test'), (75, 75, 'Ranglar Test'),    (76, 76, 'Kundalik Test'),
(81, 81, 'Salomlashish Test'), (82, 82, 'Numeros Test'), (83, 83, 'Colores Test'),
(84, 84, 'Familia Test'),  (85, 85, 'Comida Test'),     (86, 86, 'Vida Test')
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- VOCABULARY FOR NEW LESSONS (10 words each)
-- ═══════════════════════════════════════════════════════

INSERT INTO vocabulary (id, lesson_id, translation_uz, translation_target, order_index) VALUES
-- Russian: Lesson 21
(101, 21, 'Salom',       'Привет',          1), (102, 21, 'Xayr',      'Пока',           2),
(103, 21, 'Rahmat',      'Спасибо',         3), (104, 21, 'Ha',        'Да',             4),
(105, 21, 'Yo''q',       'Нет',             5), (106, 21, 'Iltimos',   'Пожалуйста',     6),
(107, 21, 'Kechirasiz',  'Извините',        7), (108, 21, 'Yaxshi',    'Хорошо',         8),
(109, 21, 'Yomon',       'Плохо',           9), (110, 21, 'Men',       'Я',              10),
-- Russian: Lesson 23
(111, 23, 'Salom (rasmiy)', 'Здравствуйте', 1), (112, 23, 'Ko''rishguncha', 'До свидания',2),
(113, 23, 'Isming nima?', 'Как тебя зовут?',3), (114, 23, 'Mening ismim', 'Меня зовут',  4),
(115, 23, 'Qanday?',     'Как?',            5), (116, 23, 'Juda yaxshi', 'Очень хорошо', 6),
(117, 23, 'Necha yoshda?','Сколько лет?',   7), (118, 23, 'Men ... yoshdaman','Мне ... лет',8),
(119, 23, 'Qayerdansan?', 'Откуда ты?',    9), (120, 23, 'O''zbek',   'Узбек',         10),
-- Turkish: Lesson 31
(131, 31, 'Salom',       'Merhaba',         1), (132, 31, 'Xayr',      'Güle güle',      2),
(133, 31, 'Rahmat',      'Teşekkür ederim', 3), (134, 31, 'Ha',        'Evet',           4),
(135, 31, 'Yo''q',       'Hayır',           5), (136, 31, 'Iltimos',   'Lütfen',         6),
(137, 31, 'Kechirasiz',  'Özür dilerim',    7), (138, 31, 'Yaxshi',    'İyi',            8),
(139, 31, 'Yomon',       'Kötü',            9), (140, 31, 'Men',       'Ben',            10),
-- German: Lesson 41
(141, 41, 'Salom',       'Hallo',           1), (142, 41, 'Xayr',      'Tschüss',        2),
(143, 41, 'Rahmat',      'Danke',           3), (144, 41, 'Ha',        'Ja',             4),
(145, 41, 'Yo''q',       'Nein',            5), (146, 41, 'Iltimos',   'Bitte',          6),
(147, 41, 'Kechirasiz',  'Entschuldigung',  7), (148, 41, 'Yaxshi',    'Gut',            8),
(149, 41, 'Yomon',       'Schlecht',        9), (150, 41, 'Men',       'Ich',            10),
-- Korean: Lesson 51
(151, 51, 'Salom',       '안녕하세요',       1), (152, 51, 'Xayr',      '안녕히 가세요',   2),
(153, 51, 'Rahmat',      '감사합니다',       3), (154, 51, 'Ha',        '네',             4),
(155, 51, 'Yo''q',       '아니요',          5), (156, 51, 'Iltimos',   '제발',           6),
(157, 51, 'Men',         '나',              7), (158, 51, 'Sen',       '당신',           8),
(159, 51, 'U',           '그',              9), (160, 51, 'Biz',       '우리',           10),
-- Arabic: Lesson 61
(161, 61, 'Salom',       'مرحبا',           1), (162, 61, 'Xayr',      'مع السلامة',     2),
(163, 61, 'Rahmat',      'شكرًا',           3), (164, 61, 'Ha',        'نعم',            4),
(165, 61, 'Yo''q',       'لا',              5), (166, 61, 'Iltimos',   'من فضلك',        6),
(167, 61, 'Men',         'أنا',             7), (168, 61, 'Sen',       'أنت',            8),
(169, 61, 'U',           'هو',              9), (170, 61, 'Biz',       'نحن',            10),
-- Chinese: Lesson 71
(171, 71, 'Salom',       '你好 (Nǐ hǎo)',   1), (172, 71, 'Xayr',      '再见 (Zàijiàn)', 2),
(173, 71, 'Rahmat',      '谢谢 (Xièxiè)',   3), (174, 71, 'Ha',        '是 (Shì)',       4),
(175, 71, 'Yo''q',       '不 (Bù)',         5), (176, 71, 'Men',       '我 (Wǒ)',        6),
(177, 71, 'Sen',         '你 (Nǐ)',         7), (178, 71, 'U',         '他 (Tā)',        8),
(179, 71, 'Biz',         '我们 (Wǒmen)',    9), (180, 71, 'Ismin nima?','你叫什么?',     10),
-- Spanish: Lesson 81
(181, 81, 'Salom',       'Hola',            1), (182, 81, 'Xayr',      'Adiós',          2),
(183, 81, 'Rahmat',      'Gracias',         3), (184, 81, 'Ha',        'Sí',             4),
(185, 81, 'Yo''q',       'No',              5), (186, 81, 'Iltimos',   'Por favor',      6),
(187, 81, 'Kechirasiz',  'Perdón',          7), (188, 81, 'Men',       'Yo',             8),
(189, 81, 'Sen',         'Tú',              9), (190, 81, 'Yaxshi',    'Bien',           10)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- QUESTIONS FOR NEW LESSONS (4 per lesson)
-- ═══════════════════════════════════════════════════════

INSERT INTO questions (id, test_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
-- Russian: test 21
(101, 21, '"Salom" ruscha?',    'Привет',   'Пока',      'Да',         'A', 1),
(102, 21, '"Rahmat" ruscha?',   'Нет',      'Спасибо',   'Привет',     'B', 2),
(103, 21, '"Ha" ruscha?',       'Нет',      'Да',        'Хорошо',     'B', 3),
(104, 21, '"Xayr" ruscha?',     'Пока',     'Привет',    'Спасибо',    'A', 4),
-- Russian: test 23
(111, 23, '"Yaxshi" ruscha?',   'Плохо',    'Хорошо',    'Нет',        'B', 1),
(112, 23, '"Yo''q" ruscha?',    'Да',       'Нет',       'Хорошо',     'B', 2),
(113, 23, '"Iltimos" ruscha?',  'Извините', 'Пожалуйста','Спасибо',    'B', 3),
(114, 23, '"Kechirasiz" ruscha?','Пожалуйста','Привет',  'Извините',   'C', 4),
-- Turkish: test 31
(131, 31, '"Salom" turkcha?',   'Teşekkür', 'Merhaba',   'Güle güle',  'B', 1),
(132, 31, '"Rahmat" turkcha?',  'Lütfen',   'Evet',      'Teşekkür ederim','C',2),
(133, 31, '"Ha" turkcha?',      'Hayır',    'Evet',      'Lütfen',     'B', 3),
(134, 31, '"Xayr" turkcha?',    'Merhaba',  'Lütfen',    'Güle güle',  'C', 4),
-- German: test 41
(141, 41, '"Salom" nemischa?',  'Danke',    'Hallo',     'Tschüss',    'B', 1),
(142, 41, '"Rahmat" nemischa?', 'Bitte',    'Danke',     'Hallo',      'B', 2),
(143, 41, '"Ha" nemischa?',     'Nein',     'Ja',        'Bitte',      'B', 3),
(144, 41, '"Xayr" nemischa?',   'Hallo',    'Danke',     'Tschüss',    'C', 4),
-- Korean: test 51
(151, 51, '"Salom" koreyscha?', '감사합니다','안녕하세요', '아니요',     'B', 1),
(152, 51, '"Rahmat" koreyscha?','안녕하세요','감사합니다', '네',         'B', 2),
(153, 51, '"Ha" koreyscha?',    '아니요',   '네',         '감사합니다', 'B', 3),
(154, 51, '"Yo''q" koreyscha?', '네',       '아니요',     '감사합니다', 'B', 4),
-- Arabic: test 61
(161, 61, '"Salom" arabcha?',   'شكرًا',    'مرحبا',     'نعم',        'B', 1),
(162, 61, '"Rahmat" arabcha?',  'نعم',      'شكرًا',     'مرحبا',      'B', 2),
(163, 61, '"Ha" arabcha?',      'لا',       'نعم',       'شكرًا',      'B', 3),
(164, 61, '"Xayr" arabcha?',    'مرحبا',    'شكرًا',     'مع السلامة', 'C', 4),
-- Chinese: test 71
(171, 71, '"Salom" xitoycha?',  '谢谢',     '你好',       '再见',       'B', 1),
(172, 71, '"Rahmat" xitoycha?', '你好',     '再见',       '谢谢',       'C', 2),
(173, 71, '"Xayr" xitoycha?',   '再见',     '你好',       '谢谢',       'A', 3),
(174, 71, '"Men" xitoycha?',    '你',       '我',         '他',         'B', 4),
-- Spanish: test 81
(181, 81, '"Salom" ispancha?',  'Gracias',  'Hola',      'Adiós',      'B', 1),
(182, 81, '"Rahmat" ispancha?', 'Hola',     'Adiós',     'Gracias',    'C', 2),
(183, 81, '"Ha" ispancha?',     'No',       'Sí',        'Por favor',  'B', 3),
(184, 81, '"Xayr" ispancha?',   'Hola',     'Gracias',   'Adiós',      'C', 4)
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- EXERCISES FOR NEW LESSONS (3 per lesson)
-- ═══════════════════════════════════════════════════════

INSERT INTO exercises (id, lesson_id, name, order_index, sentence, options, correct_answer) VALUES
-- Russian: Lesson 21
(101, 21, 'Fill', 1, '___ ! Как дела? (Salom)',          'Привет,Пока,Нет',          'Привет'),
(102, 21, 'Fill', 2, '___ (Rahmat)',                      'Нет,Спасибо,Привет',       'Спасибо'),
(103, 21, 'Fill', 3, '___ (Xayr)',                        'Привет,Да,Пока',           'Пока'),
-- Russian: Lesson 23
(111, 23, 'Fill', 1, 'Меня ___ Али (ismim ... deyishadi)','зовут,пока,нет',           'зовут'),
(112, 23, 'Fill', 2, '___ из Узбекистана (men)',          'Я,Ты,Он',                  'Я'),
(113, 23, 'Fill', 3, 'До ___! (Ko''rishguncha)',           'привет,свидания,да',       'свидания'),
-- Turkish: Lesson 31
(131, 31, 'Fill', 1, '___ ! Nasılsın? (Salom)',           'Merhaba,Güle güle,Hayır',  'Merhaba'),
(132, 31, 'Fill', 2, '___ (Rahmat)',                      'Lütfen,Teşekkür ederim,Evet','Teşekkür ederim'),
(133, 31, 'Fill', 3, '___ (Xayr)',                        'Merhaba,Güle güle,Evet',   'Güle güle'),
-- German: Lesson 41
(141, 41, 'Fill', 1, '___ ! Wie geht es? (Salom)',        'Hallo,Tschüss,Nein',       'Hallo'),
(142, 41, 'Fill', 2, '___ (Rahmat)',                      'Bitte,Danke,Hallo',        'Danke'),
(143, 41, 'Fill', 3, '___ (Xayr)',                        'Hallo,Danke,Tschüss',      'Tschüss'),
-- Korean: Lesson 51
(151, 51, 'Fill', 1, '___ (Salom)',                       '안녕하세요,아니요,감사합니다', '안녕하세요'),
(152, 51, 'Fill', 2, '___ (Ha)',                          '네,아니요,감사합니다',      '네'),
(153, 51, 'Fill', 3, '___ (Rahmat)',                      '안녕하세요,감사합니다,아니요','감사합니다'),
-- Arabic: Lesson 61
(161, 61, 'Fill', 1, '___ ! كيف حالك؟ (Salom)',           'مرحبا,شكرًا,نعم',          'مرحبا'),
(162, 61, 'Fill', 2, '___ (Rahmat)',                      'نعم,لا,شكرًا',             'شكرًا'),
(163, 61, 'Fill', 3, '___ (Ha)',                          'لا,نعم,شكرًا',             'نعم'),
-- Chinese: Lesson 71
(171, 71, 'Fill', 1, '___ (Salom)',                       '你好,再见,谢谢',            '你好'),
(172, 71, 'Fill', 2, '___ (Rahmat)',                      '你好,再见,谢谢',            '谢谢'),
(173, 71, 'Fill', 3, '___ (Xayr)',                        '你好,再见,谢谢',            '再见'),
-- Spanish: Lesson 81
(181, 81, 'Fill', 1, '___ ! ¿Cómo estás? (Salom)',        'Hola,Adiós,No',            'Hola'),
(182, 81, 'Fill', 2, '___ (Rahmat)',                      'Hola,Gracias,Sí',          'Gracias'),
(183, 81, 'Fill', 3, '___ (Xayr)',                        'Hola,Adiós,Gracias',       'Adiós')
ON CONFLICT (id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- ENROLL TEST USER IN RUSSIAN BEGINNER
-- ═══════════════════════════════════════════════════════

INSERT INTO user_courses (user_id, course_id, progress, created_at)
VALUES (1, 5, 0.00, NOW())
ON CONFLICT (user_id, course_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- BOOKS TABLE
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS books (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    author          VARCHAR(255),
    category        VARCHAR(50),          -- 'digital' or 'print'
    description     TEXT,
    price           INTEGER DEFAULT 0,    -- in so'm
    price_label     VARCHAR(100),         -- "45,000 so'm"
    is_free         BOOLEAN DEFAULT FALSE,
    emoji           VARCHAR(20) DEFAULT '📚',
    cover_color1    INTEGER,              -- ARGB int
    cover_color2    INTEGER,
    pages           VARCHAR(50),          -- "380 bet"
    page_count      INTEGER DEFAULT 0,
    format          VARCHAR(50),          -- pdf, epub, hardcover, paperback
    rating          DECIMAL(3,1) DEFAULT 4.5,
    review_count    INTEGER DEFAULT 0,
    language        VARCHAR(100),
    level           VARCHAR(100),
    preview_pages   TEXT,                 -- JSON array
    image_id        INTEGER,
    file_id         INTEGER,
    file_path       VARCHAR(500),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_books (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id         BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    payment_method  VARCHAR(50),
    is_active       BOOLEAN DEFAULT TRUE,
    read_page       INTEGER DEFAULT 0,
    total_pages     INTEGER DEFAULT 0,
    purchased_at    TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, book_id)
);

-- ═══════════════════════════════════════════════════════
-- MIGRATE: Add file_path column to books (for existing DBs)
-- ═══════════════════════════════════════════════════════
ALTER TABLE books ADD COLUMN IF NOT EXISTS file_path VARCHAR(500);
ALTER TABLE user_books ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE user_books ADD COLUMN IF NOT EXISTS read_page INTEGER DEFAULT 0;
ALTER TABLE user_books ADD COLUMN IF NOT EXISTS total_pages INTEGER DEFAULT 0;

-- ═══════════════════════════════════════════════════════
-- BOOKS SEED DATA
-- ═══════════════════════════════════════════════════════

INSERT INTO books (id, title, author, category, description, price, price_label, is_free, emoji, cover_color1, cover_color2, pages, page_count, format, rating, review_count, language, level, preview_pages, file_path) VALUES
-- Digital books
(1,  'English Grammar In Use', 'Raymond Murphy', 'digital',
 'Ingliz tili grammatikasini o''rganuvchilar uchun eng ko''p sotilgan kitob. Amaliy mashqlar, misollar va aniq tushuntirishlar bilan to''ldirilgan. Cambridge University Press tomonidan nashr etilgan.',
 0, 'Bepul', true, '📗', 769671790, 528237397, '380 bet', 380, 'pdf', 4.9, 1240, 'Ingliz', 'O''rta',
 '["Grammatika — mavzular ro''yxati","Present Simple: Men har kuni maktabga boraman.","Past Simple: Kecha men filmga bordim.","Future Simple: Ertaga men do''stim bilan uchrashaman."]',
 'books/english_grammar.pdf'),

(2,  'Oxford Word Skills', 'Ruth Gairns', 'digital',
 'Ingliz lug''atini tizimli ravishda kengaytirish uchun ideal kitob. 3 darajada (Boshlang''ich, O''rta, Ilg''or) mavjud. Ko''rgazmali va o''yin-o''rganuv usulida qurilgan.',
 45000, '45,000 so''m', false, '📘', 586612709, 444653997, '256 bet', 256, 'epub', 4.7, 873, 'Ingliz', 'Boshlang''ich – Ilg''or',
 '["1-bob: Odamlar va shaxsiyat so''zlari","Mashq 1: Bo''shliqlarni to''ldiring.","Mashq 2: Rasmga qarab javob yozing."]',
 NULL),

(3,  'Business English', 'David Cotton', 'digital',
 'Biznes ingliz tilini o''rganish uchun mo''ljallangan professional kurs kitobida elektron pochta yozish, prezentatsiya qilish, muzokaralar olib borish ko''nikmalari mavjud.',
 60000, '60,000 so''m', false, '📙', -356762360, -1266410227, '304 bet', 304, 'pdf', 4.6, 542, 'Ingliz', 'O''rta – Ilg''or',
 '["1-bob: Prezentatsiya qilish","Misol: Good morning, today I will present...","Mashq: Elektron pochta yozing."]',
 'books/business_english.pdf'),

(4,  'Everyday Conversations', 'US State Dept.', 'digital',
 'AQSh Davlat departamenti tomonidan nashr qilingan bepul ingliz tili suhbat darsligi. Kundalik hayotda kerakli so''z va iboralar bilan to''ldirilgan.',
 0, 'Bepul', true, '📕', -5862726, -8627521, '128 bet', 128, 'pdf', 4.5, 2103, 'Ingliz', 'Boshlang''ich',
 '["1-dars: Salomlashish","2-dars: Do''stingizni tanishtirish","3-dars: Ish haqida suhbat"]',
 'books/uzbek_english_dict.pdf'),

(5,  'IELTS Preparation', 'Cambridge ESOL', 'digital',
 'IELTS imtihoniga tayyorlanish uchun to''liq qo''llanma. Reading, Writing, Listening va Speaking bo''limlari uchun amaliy mashqlar va strategiyalar.',
 80000, '80,000 so''m', false, '📚', -7828891, -1885899, '420 bet', 420, 'pdf', 4.8, 1870, 'Ingliz', 'O''rta – Yuqori',
 '["IELTS formatini tushunish","Band 7+ olish strategiyalari","Reading: Tezkor o''qish texnikasi"]',
 'books/ielts_preparation.pdf'),

(6,  'Speak Naturally', 'K. Johnson', 'digital',
 'So''zlashuvchi ingliz tilini tabiiy ravishda o''rgatuvchi kitob. Ingliz ona-tili sifatidagi odamlar kabi gapirish uchun fonetika, intonatsiya va idiomalar.',
 35000, '35,000 so''m', false, '🗣️', 1538343822, 780566126, '192 bet', 192, 'epub', 4.4, 321, 'Ingliz', 'O''rta',
 '["1-bob: Fonetika asoslari","Intonatsiya: so''roq va darak gaplar","Ko''p ishlatiladigan idiomalar ro''yxati"]',
 NULL),

-- Print books
(7,  'Headway Upper-Int.', 'John & Liz Soars', 'print',
 'Jahonda eng ko''p ishlatiladigan ingliz tili darsligi. Tizimli grammatika, so''z boyligi va ko''nikma mashqlari bilan to''ldirilgan.',
 120000, '120,000 so''m', false, '📗', 528237397, 260506165, '192 bet', 192, 'hardcover', 4.8, 3201, 'Ingliz', 'Yuqori O''rta',
 '["Unit 1: The world around us","Grammar Focus: Past tenses","Skills: Listening & Speaking"]',
 NULL),

(8,  'Cambridge IELTS 16', 'Cambridge ESOL', 'print',
 'IELTS imtihonining haqiqiy savollari bilan 4 ta to''liq imtihon variantini o''z ichiga oladi. Audio CD va javob kaliti bilan birga.',
 150000, '150,000 so''m', false, '📘', 586612709, 444653997, '240 bet', 240, 'paperback', 4.9, 4521, 'Ingliz', 'O''rta – Yuqori',
 '["Test 1: Reading Section","Test 1: Writing Task 1 va 2","Javob kaliti va band descriptors"]',
 NULL),

(9,  'English Vocabulary in Use', 'Michael McCarthy', 'print',
 'So''z boyligini kengaytirish uchun Cambridge chiqaradigan maqbul qo''llanma. Har sahifada yangi so''zlar, misollar va mashqlar.',
 95000, '95,000 so''m', false, '📙', -356762360, -1266410227, '328 bet', 328, 'paperback', 4.7, 1893, 'Ingliz', 'O''rta',
 '["Unit 1: Learning vocabulary","Unit 2: Collocation so''z birikmalari","Mashq: Synonymlar toping"]',
 NULL),

(10, 'Practical English Usage', 'L. Alexander', 'print',
 'Grammatika qoidalari va amaliy qo''llanilishini tushuntiruvchi keng qamrovli ma''lumotnoma. Inglizcha yozish va gapirish uchun zarur.',
 110000, '110,000 so''m', false, '📕', -7828891, -1885899, '288 bet', 288, 'hardcover', 4.6, 1120, 'Ingliz', 'Barcha darajalar',
 '["A-Z: Grammatika qoidalari","Misol: Articles (a, an, the) ishlatilishi","Mashq: Xatolarni toping"]',
 NULL)
ON CONFLICT (id) DO NOTHING;

-- Update file_path for existing digital PDF books
UPDATE books SET file_path = 'books/english_grammar.pdf' WHERE id = 1 AND file_path IS NULL;
UPDATE books SET file_path = 'books/business_english.pdf' WHERE id = 3 AND file_path IS NULL;
UPDATE books SET file_path = 'books/uzbek_english_dict.pdf' WHERE id = 4 AND file_path IS NULL;
UPDATE books SET file_path = 'books/ielts_preparation.pdf' WHERE id = 5 AND file_path IS NULL;

-- Give test user (id=1) books 1 and 4 (the free ones)
INSERT INTO user_books (user_id, book_id, payment_method, purchased_at)
VALUES (1, 1, 'free', NOW()), (1, 4, 'free', NOW())
ON CONFLICT (user_id, book_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- COURSE TEACHERS (many-to-many)
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS course_teachers (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    teacher_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(course_id, teacher_id)
);

-- ═══════════════════════════════════════════════════════
-- CHAT MESSAGES TABLE (WebSocket chat per course)
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS chat_messages (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_role VARCHAR(20) NOT NULL DEFAULT 'user',
    text        TEXT NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_course_student ON chat_messages(course_id, student_id, created_at);

-- ═══════════════════════════════════════════════════════
-- ADMIN & TEACHER SEED USERS
-- ═══════════════════════════════════════════════════════

-- Admin user (password = "password")
INSERT INTO users (id, first_name, last_name, email, phone, password, role, ball, created_at)
VALUES (100, 'Admin', 'OAZIS', 'admin@oazis.uz', '+998900000000',
        '$2a$10$nYv8BK48mD2iAzR4X3OibeEvAG777fl46h2YU/MdvZiDO5Un4g2si', 'ADMIN', 0, NOW())
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

-- Teacher user (password = "password")
INSERT INTO users (id, first_name, last_name, email, phone, password, role, ball, specialization, created_at)
VALUES (101, 'Nodira', 'Karimova', 'teacher@oazis.uz', '+998900000001',
        '$2a$10$nYv8BK48mD2iAzR4X3OibeEvAG777fl46h2YU/MdvZiDO5Un4g2si', 'TEACHER', 0, 'Ingliz tili', NOW())
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password;

-- Assign teacher to English courses
INSERT INTO course_teachers (course_id, teacher_id)
VALUES (1, 101), (2, 101), (3, 101)
ON CONFLICT (course_id, teacher_id) DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- NOTIFICATIONS
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(500) NOT NULL,
    body        TEXT,
    type        VARCHAR(50) DEFAULT 'GENERAL',
    ref_id      BIGINT,
    is_read     BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- FCM token storage
ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(500);

-- Seed notifications
INSERT INTO notifications (user_id, title, body, type, is_read, created_at)
VALUES
    (101, 'Yangi o''quvchi', 'Test Student Ingliz tili kursiga yozildi', 'NEW_STUDENT', false, NOW() - INTERVAL '2 hours'),
    (101, 'Kurs yangilandi', 'Ingliz tili — Boshlang''ich kursiga yangi dars qo''shildi', 'COURSE_UPDATE', false, NOW() - INTERVAL '5 hours'),
    (101, 'Tizim xabari', 'Xush kelibsiz OAZIS platformasiga!', 'GENERAL', true, NOW() - INTERVAL '1 day'),
    (100, 'Yangi o''qituvchi', 'Nodira Karimova platformaga qo''shildi', 'NEW_TEACHER', false, NOW() - INTERVAL '1 hour'),
    (100, 'Hisobot tayyor', 'Haftalik hisobot tayyorlandi', 'REPORT', true, NOW() - INTERVAL '3 hours'),
    (100, 'Tizim xabari', 'Xush kelibsiz OAZIS admin paneliga!', 'GENERAL', true, NOW() - INTERVAL '1 day')
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════
-- MIGRATE: Score'larni foizli tizimga o'tkazish
-- Eski score'lar raw count edi (masalan: 10, 4, 3)
-- Yangi tizim: 0-100 foiz (masalan: 100%, 80%, 75%)
-- Agar score 100 dan kichik va lesson uchun content bor — foizga aylantirish
-- ═══════════════════════════════════════════════════════
UPDATE user_lessons ul SET vocab_score = LEAST(100, CASE
    WHEN ul.vocab_score IS NULL THEN NULL
    WHEN ul.vocab_score > 100 THEN ul.vocab_score
    ELSE (ul.vocab_score * 100) / GREATEST(1, (SELECT COUNT(*) FROM vocabulary v WHERE v.lesson_id = ul.lesson_id))
END) WHERE ul.vocab_score IS NOT NULL AND ul.vocab_score <= 20;

UPDATE user_lessons ul SET test_score = LEAST(100, CASE
    WHEN ul.test_score IS NULL THEN NULL
    WHEN ul.test_score > 100 THEN ul.test_score
    ELSE (ul.test_score * 100) / GREATEST(1, (SELECT COUNT(*) FROM questions q JOIN tests t ON t.id = q.test_id WHERE t.lesson_id = ul.lesson_id))
END) WHERE ul.test_score IS NOT NULL AND ul.test_score <= 20;

UPDATE user_lessons ul SET exercise_score = LEAST(100, CASE
    WHEN ul.exercise_score IS NULL THEN NULL
    WHEN ul.exercise_score > 100 THEN ul.exercise_score
    ELSE (ul.exercise_score * 100) / GREATEST(1, (SELECT COUNT(*) FROM exercises e WHERE e.lesson_id = ul.lesson_id))
END) WHERE ul.exercise_score IS NOT NULL AND ul.exercise_score <= 20;

-- ═══════════════════════════════════════════════════════
-- FIX: BIGSERIAL sequence after explicit ID inserts
-- ═══════════════════════════════════════════════════════
SELECT setval('points_id_seq',      COALESCE((SELECT MAX(id) FROM points), 0));
SELECT setval('users_id_seq',       COALESCE((SELECT MAX(id) FROM users), 0));
SELECT setval('languages_id_seq',   COALESCE((SELECT MAX(id) FROM languages), 0));
SELECT setval('courses_id_seq',     COALESCE((SELECT MAX(id) FROM courses), 0));
SELECT setval('lessons_id_seq',     COALESCE((SELECT MAX(id) FROM lessons), 0));
SELECT setval('tests_id_seq',       COALESCE((SELECT MAX(id) FROM tests), 0));
SELECT setval('vocabulary_id_seq',  COALESCE((SELECT MAX(id) FROM vocabulary), 0));
SELECT setval('questions_id_seq',   COALESCE((SELECT MAX(id) FROM questions), 0));
SELECT setval('exercises_id_seq',   COALESCE((SELECT MAX(id) FROM exercises), 0));
SELECT setval('user_lessons_id_seq',COALESCE((SELECT MAX(id) FROM user_lessons), 0));
SELECT setval('user_courses_id_seq',COALESCE((SELECT MAX(id) FROM user_courses), 0));
SELECT setval('books_id_seq',       COALESCE((SELECT MAX(id) FROM books), 0));

-- ═══════════════════════════════════════════════════════
-- ACHIEVEMENTS TABLE
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS achievements (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    icon        VARCHAR(50) DEFAULT 'star',
    bonus_points INT DEFAULT 0,
    condition_type VARCHAR(50) NOT NULL,
    condition_value INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_achievements (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id  BIGINT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
    unlocked_at     TIMESTAMP DEFAULT NOW(),
    UNIQUE (user_id, achievement_id)
);

-- ═══════════════════════════════════════════════════════
-- ACHIEVEMENTS SEED DATA
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
