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
    avatar_url      VARCHAR(500),
    fcm_token       VARCHAR(500),
    telegram_chat_id BIGINT,
    is_default      BOOLEAN DEFAULT FALSE,
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
    description       VARCHAR(500),
    flag_emoji        VARCHAR(20),
    color_start       VARCHAR(20),
    color_end         VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS courses (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    image_id      BIGINT,
    subject_id    BIGINT REFERENCES subjects(id),
    language_id   BIGINT REFERENCES languages(id),
    cover_image   VARCHAR(500),
    flag_emoji    VARCHAR(20),
    goal          VARCHAR(255),
    is_premium    BOOLEAN DEFAULT TRUE
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
    cover_image   VARCHAR(500),
    video_url     VARCHAR(500)
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
    vocab_score     INT,
    test_score      INT,
    exercise_score  INT,
    completed_at    TIMESTAMP,
    UNIQUE (user_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS books (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    author          VARCHAR(255),
    category        VARCHAR(50),
    description     TEXT,
    price           INTEGER DEFAULT 0,
    price_label     VARCHAR(100),
    is_free         BOOLEAN DEFAULT FALSE,
    emoji           VARCHAR(20) DEFAULT '📚',
    cover_color1    INTEGER,
    cover_color2    INTEGER,
    pages           VARCHAR(50),
    page_count      INTEGER DEFAULT 0,
    format          VARCHAR(50),
    rating          DECIMAL(3,1) DEFAULT 4.5,
    review_count    INTEGER DEFAULT 0,
    language        VARCHAR(100),
    level           VARCHAR(100),
    preview_pages   TEXT,
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

CREATE TABLE IF NOT EXISTS course_teachers (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    teacher_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(course_id, teacher_id)
);

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

CREATE TABLE IF NOT EXISTS click_transaction (
    id                   BIGSERIAL PRIMARY KEY,
    click_trans_id       BIGINT        NOT NULL,
    click_paydoc_id      BIGINT        NOT NULL,
    merchant_trans_id    VARCHAR(255)  NOT NULL,
    merchant_prepare_id  BIGINT,
    amount               NUMERIC(18,2) NOT NULL,
    status               VARCHAR(50)   NOT NULL DEFAULT 'CREATED',
    sign_time            VARCHAR(50),
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS orders (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT,
    product_id     BIGINT        NOT NULL,
    product_type   VARCHAR(20)   NOT NULL,
    amount         NUMERIC(18,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payme_transaction (
    id                  BIGSERIAL PRIMARY KEY,
    payme_id            VARCHAR(255)  NOT NULL UNIQUE,
    merchant_trans_id   VARCHAR(255)  NOT NULL,
    amount              NUMERIC(18,2) NOT NULL,
    status              VARCHAR(50)   NOT NULL DEFAULT 'CREATED',
    create_time         BIGINT,
    perform_time        BIGINT,
    cancel_time         BIGINT,
    cancel_reason       INTEGER,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS paynet_transaction (
    id                  BIGSERIAL PRIMARY KEY,
    transaction_id      VARCHAR(255)  NOT NULL UNIQUE,
    merchant_trans_id   VARCHAR(255)  NOT NULL,
    amount              NUMERIC(18,2) NOT NULL,
    status              VARCHAR(50)   NOT NULL DEFAULT 'CREATED',
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS uzum_nasiya_transaction (
    id                  BIGSERIAL PRIMARY KEY,
    trans_id            VARCHAR(255)  NOT NULL UNIQUE,
    merchant_trans_id   VARCHAR(255)  NOT NULL,
    amount              NUMERIC(18,2) NOT NULL,
    status              VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS alif_transaction (
    id                  BIGSERIAL PRIMARY KEY,
    bepaid_uid          VARCHAR(255),
    merchant_trans_id   VARCHAR(255)  NOT NULL,
    amount              NUMERIC(18,2) NOT NULL,
    currency            VARCHAR(10)   NOT NULL DEFAULT 'UZS',
    status              VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sale_records (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    book_id           BIGINT NOT NULL REFERENCES books(id),
    book_title        VARCHAR(500),
    buyer_name        VARCHAR(500),
    amount            INTEGER NOT NULL DEFAULT 0,
    payment_method    VARCHAR(100) NOT NULL,
    transaction_id    VARCHAR(255),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
