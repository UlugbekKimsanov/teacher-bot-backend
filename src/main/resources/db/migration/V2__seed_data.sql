-- =============================================
-- V2: Seed data
-- =============================================

-- Subjects
INSERT INTO subjects (name) VALUES
    ('Chet tillari'),
    ('IT'),
    ('SMM'),
    ('Mobilografiya'),
    ('Dizayn');

-- Courses
INSERT INTO courses (name, subject_id) VALUES
    ('Ingliz tili',  (SELECT id FROM subjects WHERE name = 'Chet tillari')),
    ('Nemis tili',   (SELECT id FROM subjects WHERE name = 'Chet tillari')),
    ('Rus tili',     (SELECT id FROM subjects WHERE name = 'Chet tillari')),
    ('Turk tili',    (SELECT id FROM subjects WHERE name = 'Chet tillari')),
    ('Xitoy tili',   (SELECT id FROM subjects WHERE name = 'Chet tillari')),
    ('Koreys tili',  (SELECT id FROM subjects WHERE name = 'Chet tillari')),
    ('IT Asoslari',  (SELECT id FROM subjects WHERE name = 'IT')),
    ('SMM',          (SELECT id FROM subjects WHERE name = 'SMM')),
    ('Mobilografiya',(SELECT id FROM subjects WHERE name = 'Mobilografiya')),
    ('Grafik Dizayn',(SELECT id FROM subjects WHERE name = 'Dizayn')),
    ('Ui/Ux Dizayn', (SELECT id FROM subjects WHERE name = 'Dizayn'));

-- Lessons (Ingliz tili course)
INSERT INTO lessons (course_id, name, order_index, duration_sec) VALUES
    (1, '1-dars', 1, 335),
    (1, '2-dars', 2, 420),
    (1, '3-dars', 3, 380),
    (1, '4-dars', 4, 400),
    (1, '5-dars', 5, 360),
    (1, '6-dars', 6, 390);

-- Vocabulary (1-dars)
INSERT INTO vocabulary (lesson_id, translation_uz, translation_target, order_index) VALUES
    (1, 'Assalomu alaykum / Salom!',              'Hello / Hi',          1),
    (1, 'Zo''r',                                   'Fine',               2),
    (1, 'Tanishganimdan hursandman.',              'Nice to meet you.',   3),
    (1, 'Sening/sizning isming/ismingiz nima?',    'What is your name?',  4),
    (1, 'Rahmat',                                  'Thanks',             5),
    (1, 'Men ham.',                                'Me too.',            6),
    (1, 'Ishlar qalay?',                           'How are you?',       7),
    (1, 'Qayyerdansiz?',                           'Where are you from?', 8),
    (1, 'Xayr!',                                   'Good bye!',          9);

-- Questions (1-dars)
INSERT INTO questions (lesson_id, question_text, option_a, option_b, option_c, correct_option, order_index) VALUES
    (1, 'She ___ a doctor.',  'am', 'is', 'are', 'B', 1),
    (1, 'I ___ happy.',       'am', 'is', 'are', 'A', 2),
    (1, 'They ___ at school.','am', 'is', 'are', 'C', 3);
