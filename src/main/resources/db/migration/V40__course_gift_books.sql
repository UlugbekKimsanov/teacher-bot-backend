-- Kursga sovg'a kitob biriktirish: kurs sotib olinganda biriktirilgan kitoblar
-- o'quvchining kutubxonasiga (user_books, payment_method = 'gift') avtomatik qo'shiladi.
CREATE TABLE IF NOT EXISTS course_gift_books (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    book_id     BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    created_at  TIMESTAMP DEFAULT NOW(),
    UNIQUE (course_id, book_id)
);

CREATE INDEX IF NOT EXISTS idx_course_gift_books_book_id ON course_gift_books(book_id);
