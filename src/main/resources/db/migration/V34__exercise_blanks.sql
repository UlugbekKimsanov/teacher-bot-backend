-- Mashqda bir nechta bo'shliq: har bo'shliqning to'g'ri javobi alohida qatorda saqlanadi.
-- Gapdagi '_' guruhlari tartibi = position (0 dan boshlab).
CREATE TABLE IF NOT EXISTS exercise_blanks (
    id             BIGSERIAL PRIMARY KEY,
    exercise_id    BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    position       INT NOT NULL DEFAULT 0,
    correct_answer VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_exercise_blanks_exercise_id ON exercise_blanks(exercise_id);

-- Mavjud mashqlar: har biri bitta bo'shliqli — eski correct_answer ko'chiriladi
INSERT INTO exercise_blanks (exercise_id, position, correct_answer)
SELECT e.id, 0, e.correct_answer
FROM exercises e
WHERE e.correct_answer IS NOT NULL
  AND e.correct_answer <> ''
  AND NOT EXISTS (SELECT 1 FROM exercise_blanks b WHERE b.exercise_id = e.id);

-- Eski ustun endi yozilmaydi (to'g'ri javoblar exercise_blanks da); yangi qatorlar uchun NULL bo'ladi
ALTER TABLE exercises ALTER COLUMN correct_answer DROP NOT NULL;
