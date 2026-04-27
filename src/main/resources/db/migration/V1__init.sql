-- =============================================
-- V1: Initial schema
-- =============================================

-- Users
CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) UNIQUE,
    phone      VARCHAR(20)  UNIQUE NOT NULL,
    password   VARCHAR(255) NOT NULL,
    address    VARCHAR(500),
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Subjects (without image_id — added after files table)
CREATE TABLE subjects (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

-- Courses (without image_id — added after files table)
CREATE TABLE courses (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    subject_id BIGINT       REFERENCES subjects(id) ON DELETE SET NULL
);

-- Lessons
CREATE TABLE lessons (
    id           BIGSERIAL    PRIMARY KEY,
    course_id    BIGINT       NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    order_index  INT          NOT NULL DEFAULT 0,
    duration_sec INT          NOT NULL DEFAULT 0
);

-- Files (central file storage)
CREATE TABLE files (
    id            BIGSERIAL     PRIMARY KEY,
    path          VARCHAR(1000) NOT NULL,
    original_name VARCHAR(300)  NOT NULL,
    mime_type     VARCHAR(100)  NOT NULL,
    size          BIGINT        NOT NULL,
    lesson_id     BIGINT        REFERENCES lessons(id) ON DELETE CASCADE,
    duration      INT,
    type          VARCHAR(20)   NOT NULL CHECK (type IN ('VIDEO', 'AUDIO', 'IMAGE', 'DOCUMENT')),
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Add image references (now that files table exists)
ALTER TABLE subjects ADD COLUMN image_id BIGINT REFERENCES files(id) ON DELETE SET NULL;
ALTER TABLE courses  ADD COLUMN image_id BIGINT REFERENCES files(id) ON DELETE SET NULL;

-- Vocabulary
CREATE TABLE vocabulary (
    id                 BIGSERIAL    PRIMARY KEY,
    lesson_id          BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    translation_uz     VARCHAR(500) NOT NULL,
    translation_target VARCHAR(500) NOT NULL,
    order_index        INT          NOT NULL DEFAULT 0
);

-- Questions
CREATE TABLE questions (
    id             BIGSERIAL    PRIMARY KEY,
    lesson_id      BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    question_text  VARCHAR(500) NOT NULL,
    option_a       VARCHAR(200) NOT NULL,
    option_b       VARCHAR(200) NOT NULL,
    option_c       VARCHAR(200) NOT NULL,
    option_d       VARCHAR(200),
    correct_option VARCHAR(1)   NOT NULL CHECK (correct_option IN ('A', 'B', 'C', 'D')),
    order_index    INT          NOT NULL DEFAULT 0
);

-- Exercises
CREATE TABLE exercises (
    id             BIGSERIAL    PRIMARY KEY,
    lesson_id      BIGINT       NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    name           VARCHAR(200) NOT NULL,
    order_index    INT          NOT NULL DEFAULT 0,
    sentence       TEXT         NOT NULL,
    options        TEXT         NOT NULL,
    correct_answer VARCHAR(200) NOT NULL
);

-- User-Course enrollment
CREATE TABLE user_courses (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT        NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    progress    DECIMAL(5, 2) NOT NULL DEFAULT 0,
    enrolled_at TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, course_id)
);

-- User lesson progress
CREATE TABLE user_lessons (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id        BIGINT    NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    is_completed     BOOLEAN   NOT NULL DEFAULT FALSE,
    vocab_score      INT       NOT NULL DEFAULT 0,
    test_score       INT       NOT NULL DEFAULT 0,
    questions_score  INT       NOT NULL DEFAULT 0,
    current_play_sec INT       NOT NULL DEFAULT 0,
    completed_at     TIMESTAMP,
    UNIQUE (user_id, lesson_id)
);

-- Attendance
CREATE TABLE attendance (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT    NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    attended_at DATE      NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE (user_id, course_id, attended_at)
);

-- Points
CREATE TABLE points (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity   VARCHAR(100) NOT NULL,
    amount     INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Certificates
CREATE TABLE certificates (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id BIGINT    NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, course_id)
);

-- Teacher questions
CREATE TABLE teacher_questions (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id  BIGINT    NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    question   TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- News
CREATE TABLE news (
    id         BIGSERIAL    PRIMARY KEY,
    name       VARCHAR(300) NOT NULL,
    image_id   BIGINT       REFERENCES files(id) ON DELETE SET NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- =============================================
-- Indexes on foreign keys
-- =============================================
CREATE INDEX idx_courses_subject_id         ON courses(subject_id);
CREATE INDEX idx_courses_image_id           ON courses(image_id);
CREATE INDEX idx_lessons_course_id          ON lessons(course_id);
CREATE INDEX idx_files_lesson_id            ON files(lesson_id);
CREATE INDEX idx_subjects_image_id          ON subjects(image_id);
CREATE INDEX idx_vocabulary_lesson_id       ON vocabulary(lesson_id);
CREATE INDEX idx_questions_lesson_id        ON questions(lesson_id);
CREATE INDEX idx_exercises_lesson_id        ON exercises(lesson_id);
CREATE INDEX idx_user_courses_user_id       ON user_courses(user_id);
CREATE INDEX idx_user_courses_course_id     ON user_courses(course_id);
CREATE INDEX idx_user_lessons_user_id       ON user_lessons(user_id);
CREATE INDEX idx_user_lessons_lesson_id     ON user_lessons(lesson_id);
CREATE INDEX idx_attendance_user_id         ON attendance(user_id);
CREATE INDEX idx_attendance_course_id       ON attendance(course_id);
CREATE INDEX idx_points_user_id             ON points(user_id);
CREATE INDEX idx_certificates_user_id       ON certificates(user_id);
CREATE INDEX idx_certificates_course_id     ON certificates(course_id);
CREATE INDEX idx_teacher_questions_user_id  ON teacher_questions(user_id);
CREATE INDEX idx_teacher_questions_lesson_id ON teacher_questions(lesson_id);
CREATE INDEX idx_news_image_id              ON news(image_id);
