-- Users
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)  UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Courses
CREATE TABLE IF NOT EXISTS courses (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    category    VARCHAR(100) NOT NULL,
    flag_emoji  VARCHAR(10),
    hours       INT          NOT NULL DEFAULT 0,
    lesson_count INT         NOT NULL DEFAULT 0,
    goal        VARCHAR(200),
    is_premium  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Lessons
CREATE TABLE IF NOT EXISTS lessons (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT       NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    video_url   VARCHAR(500),
    order_index INT          NOT NULL DEFAULT 0,
    duration_sec INT         NOT NULL DEFAULT 0
);

-- Vocabulary
CREATE TABLE IF NOT EXISTS vocabulary (
    id          BIGSERIAL PRIMARY KEY,
    lesson_id   BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    phrase_uz   VARCHAR(500) NOT NULL,
    phrase_en   VARCHAR(500) NOT NULL,
    order_index INT          NOT NULL DEFAULT 0
);

-- Tests
CREATE TABLE IF NOT EXISTS tests (
    id          BIGSERIAL PRIMARY KEY,
    lesson_id   BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE
);

-- Questions
CREATE TABLE IF NOT EXISTS questions (
    id              BIGSERIAL PRIMARY KEY,
    test_id         BIGINT       NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    question_text   VARCHAR(500) NOT NULL,
    option_a        VARCHAR(200) NOT NULL,
    option_b        VARCHAR(200) NOT NULL,
    option_c        VARCHAR(200) NOT NULL,
    correct_option  CHAR(1)      NOT NULL CHECK (correct_option IN ('A','B','C')),
    order_index     INT          NOT NULL DEFAULT 0
);

-- User ↔ Course enrollment
CREATE TABLE IF NOT EXISTS user_courses (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT       NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    progress    DECIMAL(5,2) NOT NULL DEFAULT 0,
    enrolled_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, course_id)
);

-- User lesson progress
CREATE TABLE IF NOT EXISTS user_lessons (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id       BIGINT   NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    is_completed    BOOLEAN  NOT NULL DEFAULT FALSE,
    vocab_score     INT      NOT NULL DEFAULT 0,
    test_score      INT      NOT NULL DEFAULT 0,
    questions_score INT      NOT NULL DEFAULT 0,
    completed_at    TIMESTAMP,
    UNIQUE (user_id, lesson_id)
);

-- Attendance (per day)
CREATE TABLE IF NOT EXISTS attendance (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT  NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    attended_at DATE    NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE (user_id, course_id, attended_at)
);

-- Points log
CREATE TABLE IF NOT EXISTS points (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity    VARCHAR(100) NOT NULL,
    amount      INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Certificates
CREATE TABLE IF NOT EXISTS certificates (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT    NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    issued_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, course_id)
);

-- Ask teacher messages
CREATE TABLE IF NOT EXISTS teacher_questions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id   BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    question    TEXT         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS books (
                       id BIGSERIAL PRIMARY KEY,
                       title VARCHAR(255),
                       author VARCHAR(255),
                       category VARCHAR,
                       description TEXT,
                       price NUMERIC,
                       image_id NUMERIC,
                       file_id NUMERIC ,
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW()
);

-- Seed data
INSERT INTO courses (name, category, flag_emoji, hours, lesson_count, goal, is_premium) VALUES
('Ingliz tili', 'Chet tillari', '🇬🇧', 15, 42, '0 dan so''zlashuvgacha', true),
('Nemis tili',  'Chet tillari', '🇩🇪', 20, 42, '0 dan so''zlashuvgacha', true),
('Rus tili',    'Chet tillari', '🇷🇺', 15, 42, '0 dan so''zlashuvgacha', true),
('Turk tili',   'Chet tillari', '🇹🇷', 15, 42, '0 dan so''zlashuvgacha', true),
('Xitoy tili',  'Chet tillari', '🇨🇳', 15, 42, '0 dan so''zlashuvgacha', true),
('Koreys tili', 'Chet tillari', '🇰🇷', 15, 42, '0 dan so''zlashuvgacha', true),
('IT Asoslari', 'IT',           '💻',  20, 30, 'IT sohasiga kirish',    true),
('SMM',         'SMM',          '📱',  15, 25, 'Social media marketing', true),
('Mobilografiya','Mobilografiya','📷', 10, 20, 'Telefonda kasbiy foto',  true),
('Grafik Dizayn','Grafik Dizayn','🎨', 20, 35, 'Professional dizayn',   true),
('Ui/Ux Dizayn', 'Ui/Ux Dizayn','🖥️', 20, 35, 'Interfeys dizayni',     true);

INSERT INTO lessons (course_id, title, video_url, order_index, duration_sec) VALUES
(1, '1-dars', 'https://example.com/videos/en/1', 1, 335),
(1, '2-dars', 'https://example.com/videos/en/2', 2, 420),
(1, '3-dars', 'https://example.com/videos/en/3', 3, 380),
(1, '4-dars', 'https://example.com/videos/en/4', 4, 400),
(1, '5-dars', 'https://example.com/videos/en/5', 5, 360),
(1, '6-dars', 'https://example.com/videos/en/6', 6, 390);

INSERT INTO vocabulary (lesson_id, phrase_uz, phrase_en, order_index) VALUES
(1, 'Assalomu alaykum / Salom!', 'Hello / Hi', 1),
(1, 'Zo''r',                     'Fine',       2),
(1, 'Tanishganimdan hursandman.','Nice to meet you.', 3),
(1, 'Sening/sizning isming/ismingiz nima?', 'What is your name?', 4),
(1, 'Rahmat',                    'Thanks',     5),
(1, 'Men ham.',                  'Me too.',    6),
(1, 'Ishlar qalay?',             'How are you?', 7),
(1, 'Qayyerdansiz?',             'Where are you from?', 8),
(1, 'Xayr!',                     'Good bye!',  9);

INSERT INTO tests (lesson_id) VALUES (1);

INSERT INTO questions (test_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
(1, 'She ___ a doctor.', 'am', 'is', 'are', 'B', 1),
(1, 'I ___ happy.',      'am', 'is', 'are', 'A', 2),
(1, 'They ___ at school.','am','is', 'are', 'C', 3);
