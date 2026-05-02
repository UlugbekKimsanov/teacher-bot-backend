-- Users jadvaliga yangi ustunlar
ALTER TABLE users ADD COLUMN IF NOT EXISTS ball BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);

-- Languages jadvali
CREATE TABLE IF NOT EXISTS languages (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(200) NOT NULL UNIQUE,
    background_image  VARCHAR(500),
    flag_image        VARCHAR(500),
    description       VARCHAR(500)
);

-- Courses ga yangi ustunlar
ALTER TABLE courses ADD COLUMN IF NOT EXISTS language_id BIGINT REFERENCES languages(id);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500);

-- Lessons ga cover_image ustuni
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS cover_image VARCHAR(500);

-- Activities jadvali
CREATE TABLE IF NOT EXISTS activities (
    id              BIGSERIAL PRIMARY KEY,
    activity_type   VARCHAR(50)  NOT NULL,
    measure_type    VARCHAR(50)  NOT NULL,
    value           BIGINT       NOT NULL DEFAULT 0,
    student_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_activities_student_date ON activities(student_id, created_at);
