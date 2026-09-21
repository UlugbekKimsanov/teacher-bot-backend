-- V22 tarixini o'zgartirmasdan versionless/V1 landing JSONni kanonik V2 ga o'tkazadi.
-- Mavjud admin qiymatlari saqlanadi; faqat yo'q maydonlar backfill va eski elementlar rename qilinadi.
DO $migration$
DECLARE
    v_default jsonb := $json$
    {
      "schemaVersion": 2,
      "navbar": {
        "logo": "OAZIS",
        "links": [
          { "label": "Kurslar", "href": "#courses" },
          { "label": "Biz haqimizda", "href": "#about" },
          { "label": "Fikrlar", "href": "#testimonials" }
        ],
        "ctaLabel": "Ro'yxatdan o'tish",
        "menuLabel": "Menyu"
      },
      "hero": {
        "title": "BILIMGA BIR MARTA TO'LAYMIZ, BILIMSIZLIKKA ESA BIR UMR!",
        "subtitle": "Oazis Chet tillarini atiga 4 oy ichida interaktiv va online usulda tez va oson o'rganing va maqsadingizga erishing!",
        "primaryBtn": "Ma'lumot olish",
        "secondaryBtn": "Kirish"
      },
      "stats": [
        { "value": "1000+", "label": "o'quvchilar" },
        { "value": "7", "label": "kurslar" },
        { "value": "280+", "label": "videodarslar" }
      ],
      "featuresTitle": "Nima uchun OAZIS platformasi?",
      "features": [
        { "icon": "GraduationCap", "text": "Malakali ustozlar tomonidan to'liq video darsliklar" },
        { "icon": "ClipboardCheck", "text": "Doimiy nazorat va davomat" },
        { "icon": "MessageCircleQuestion", "text": "Mavzuga doir barcha savollarga aniq javoblar" },
        { "icon": "Clock", "text": "Istalgan paytda va istalgan joyda o'rganish" }
      ],
      "courseInfoTitle": "Kurslar haqida qisqacha",
      "courseInfo": [
        { "icon": "Video", "text": "Video va Audio darsliklar orqali chet tillarini o'rgatamiz" },
        { "icon": "Sparkles", "text": "Interaktiv va Meta ta'lim metodikalari orqali tushuntirilgan" },
        { "icon": "Bot", "text": "AI ustozdan istalgan vaqtda mavzuga doir savollarga javob bor" },
        { "icon": "Award", "text": "Kursni muvaffaqiyatli yakunlaganingizdan so'ng maxsus Sertifikat" }
      ],
      "goalsTitle": "Bizning maqsadimiz",
      "goals": [
        { "icon": "MessagesSquare", "title": "Atiga 4 oy ichida chet tillarida erkin muloqot darajasi" },
        { "icon": "Brain", "title": "Aqliy zo'riqishlarga qarshi uslubda oson ta'lim berish" },
        { "icon": "Timer", "title": "Vaqtingizni maksimal darajada tejab berish" }
      ],
      "testimonialsTitle": "O'quvchilarimiz fikrlari",
      "testimonials": [
        { "id": 1, "name": "O'quvchi fikri", "course": "Ingliz tili" },
        { "id": 2, "name": "O'quvchi fikri", "course": "Rus tili" },
        { "id": 3, "name": "O'quvchi fikri", "course": "Koreys tili" },
        { "id": 4, "name": "O'quvchi fikri", "course": "Turk tili" },
        { "id": 5, "name": "O'quvchi fikri", "course": "Arab tili" }
      ],
      "coursesTitle": "Kurslar",
      "courseCard": {
        "categoryLabel": "Til kurslari",
        "studentsLabel": "o'quvchi",
        "buyLabel": "Harid qilish"
      },
      "courses": [
        { "id": "en", "flag": "🇬🇧", "name": "Ingliz tili kursi", "students": 136, "price": "799 000 so'm", "rating": 5.0 },
        { "id": "ru", "flag": "🇷🇺", "name": "Rus tili kursi", "students": 109, "price": "799 000 so'm", "rating": 5.0 },
        { "id": "ko", "flag": "🇰🇷", "name": "Koreys tili kursi", "students": 100, "price": "799 000 so'm", "rating": 5.0 },
        { "id": "tr", "flag": "🇹🇷", "name": "Turk tili kursi", "students": 88, "price": "799 000 so'm", "rating": 5.0 },
        { "id": "ar", "flag": "🇸🇦", "name": "Arab tili kursi", "students": 72, "price": "799 000 so'm", "rating": 5.0 },
        { "id": "de", "flag": "🇩🇪", "name": "Nemis tili kursi", "students": 50, "price": "799 000 so'm", "rating": 5.0 },
        { "id": "zh", "flag": "🇨🇳", "name": "Xitoy tili kursi", "students": 33, "price": "799 000 so'm", "rating": 5.0 }
      ],
      "bonusTitle": "Bonuslar",
      "bonusText": "Harid qilgan Til kursingizning maxsus interaktiv kitoblarini bepul bonus sifatida sovg'a qilamiz!",
      "bonusBooks": [
        { "id": 1, "title": "Ingliz tili kitobi" },
        { "id": 2, "title": "Rus tili kitobi" },
        { "id": 3, "title": "Koreys tili kitobi" },
        { "id": 4, "title": "Turk tili kitobi" },
        { "id": 5, "title": "Arab tili kitobi" },
        { "id": 6, "title": "Nemis tili kitobi" },
        { "id": 7, "title": "Xitoy tili kitobi" },
        { "id": 8, "title": "Grammatika kitobi" }
      ],
      "cta": {
        "title": "Kursga qo'shilish",
        "description": "Hoziroq ro'yxatdan o'tib tanlagan kursingizga a'zo bo'ling va maqsadingizga erishing!",
        "nameLabel": "Ism",
        "phoneLabel": "Telefon raqam",
        "submitLabel": "Tasdiqlash"
      },
      "leadForm": {
        "optionalLabel": "ixtiyoriy",
        "namePlaceholder": "Ismingiz",
        "phonePlaceholder": "+998 XX XXX XX XX",
        "loadingLabel": "Yuborilmoqda...",
        "invalidPhoneMessage": "Telefon raqamini to'liq kiriting.",
        "successMessage": "Arizangiz qabul qilindi! Tez orada siz bilan bog'lanamiz.",
        "errorMessage": "Xatolik yuz berdi. Iltimos, keyinroq qayta urinib ko'ring."
      },
      "leadModal": {
        "title": "Siz bilan bog'lanamiz",
        "description": "Telefon raqamingizni qoldiring. Mutaxassisimiz sizga kurslar haqida batafsil ma'lumot beradi.",
        "closeLabel": "Yopish"
      },
      "footer": {
        "company": "Oazis Company",
        "socialTitle": "Bizning ijtimoiy sahifalar",
        "contactTitle": "Yagona aloqa markazi"
      },
      "contacts": {
        "phone": "+998 71 200 53 53",
        "telegram": "https://t.me/oazisedu",
        "instagram": "https://instagram.com/oazisedu",
        "youtube": "https://youtube.com/@oazisedu"
      }
    }
    $json$::jsonb;
    v_legacy jsonb;
    v_features jsonb;
    v_course_info jsonb;
    v_goals jsonb;
    v_courses jsonb;
    v_result jsonb;
BEGIN
    INSERT INTO landing_content (id, content)
    VALUES (1, v_default::text)
    ON CONFLICT (id) DO NOTHING;

    SELECT CASE
               WHEN jsonb_typeof(content::jsonb) = 'object' THEN content::jsonb
               ELSE '{}'::jsonb
           END
    INTO v_legacy
    FROM landing_content
    WHERE id = 1;

    IF jsonb_typeof(v_legacy -> 'features') = 'array' THEN
        SELECT COALESCE(jsonb_agg(
            (item - 'title' - 'description') || jsonb_build_object(
                'icon', COALESCE(NULLIF(item ->> 'icon', ''), CASE ord
                    WHEN 1 THEN 'GraduationCap' WHEN 2 THEN 'ClipboardCheck'
                    WHEN 3 THEN 'MessageCircleQuestion' WHEN 4 THEN 'Clock'
                    ELSE 'Sparkles' END),
                'text', COALESCE(item ->> 'text', item ->> 'title', '')
            ) ORDER BY ord
        ), '[]'::jsonb)
        INTO v_features
        FROM jsonb_array_elements(v_legacy -> 'features') WITH ORDINALITY AS entry(item, ord)
        WHERE jsonb_typeof(item) = 'object';
    ELSE
        v_features := v_default -> 'features';
    END IF;

    IF jsonb_typeof(v_legacy -> 'courseInfo') = 'array' THEN
        SELECT COALESCE(jsonb_agg(
            (item - 'title' - 'description') || jsonb_build_object(
                'icon', COALESCE(NULLIF(item ->> 'icon', ''), CASE ord
                    WHEN 1 THEN 'Video' WHEN 2 THEN 'Sparkles'
                    WHEN 3 THEN 'Bot' WHEN 4 THEN 'Award'
                    ELSE 'Sparkles' END),
                'text', COALESCE(item ->> 'text', item ->> 'title', '')
            ) ORDER BY ord
        ), '[]'::jsonb)
        INTO v_course_info
        FROM jsonb_array_elements(v_legacy -> 'courseInfo') WITH ORDINALITY AS entry(item, ord)
        WHERE jsonb_typeof(item) = 'object';
    ELSE
        v_course_info := v_default -> 'courseInfo';
    END IF;

    IF jsonb_typeof(v_legacy -> 'goals') = 'array' THEN
        SELECT COALESCE(jsonb_agg(
            (item - 'description') || jsonb_build_object(
                'icon', COALESCE(NULLIF(item ->> 'icon', ''), CASE ord
                    WHEN 1 THEN 'MessagesSquare' WHEN 2 THEN 'Brain'
                    WHEN 3 THEN 'Timer' ELSE 'Sparkles' END)
            ) ORDER BY ord
        ), '[]'::jsonb)
        INTO v_goals
        FROM jsonb_array_elements(v_legacy -> 'goals') WITH ORDINALITY AS entry(item, ord)
        WHERE jsonb_typeof(item) = 'object';
    ELSE
        v_goals := v_default -> 'goals';
    END IF;

    IF jsonb_typeof(v_legacy -> 'courses') = 'array' THEN
        SELECT COALESCE(jsonb_agg(
            (item - 'title') || jsonb_build_object(
                'id', COALESCE(NULLIF(item ->> 'id', ''), CASE ord
                    WHEN 1 THEN 'en' WHEN 2 THEN 'ru' WHEN 3 THEN 'ko'
                    WHEN 4 THEN 'tr' WHEN 5 THEN 'ar' WHEN 6 THEN 'de'
                    WHEN 7 THEN 'zh' ELSE 'legacy-' || ord::text END),
                'name', COALESCE(item ->> 'name', item ->> 'title', '')
            ) ORDER BY ord
        ), '[]'::jsonb)
        INTO v_courses
        FROM jsonb_array_elements(v_legacy -> 'courses') WITH ORDINALITY AS entry(item, ord)
        WHERE jsonb_typeof(item) = 'object';
    ELSE
        v_courses := v_default -> 'courses';
    END IF;

    v_result := v_default || v_legacy || jsonb_build_object(
        'schemaVersion', 2,
        'navbar', (v_default -> 'navbar') || CASE WHEN jsonb_typeof(v_legacy -> 'navbar') = 'object' THEN v_legacy -> 'navbar' ELSE '{}'::jsonb END,
        'hero', (v_default -> 'hero') || CASE WHEN jsonb_typeof(v_legacy -> 'hero') = 'object' THEN v_legacy -> 'hero' ELSE '{}'::jsonb END,
        'features', v_features,
        'courseInfo', v_course_info,
        'goals', v_goals,
        'courseCard', (v_default -> 'courseCard') || CASE WHEN jsonb_typeof(v_legacy -> 'courseCard') = 'object' THEN v_legacy -> 'courseCard' ELSE '{}'::jsonb END,
        'cta', (v_default -> 'cta') || CASE WHEN jsonb_typeof(v_legacy -> 'cta') = 'object' THEN v_legacy -> 'cta' ELSE '{}'::jsonb END,
        'leadForm', (v_default -> 'leadForm') || CASE WHEN jsonb_typeof(v_legacy -> 'leadForm') = 'object' THEN v_legacy -> 'leadForm' ELSE '{}'::jsonb END,
        'leadModal', (v_default -> 'leadModal') || CASE WHEN jsonb_typeof(v_legacy -> 'leadModal') = 'object' THEN v_legacy -> 'leadModal' ELSE '{}'::jsonb END,
        'footer', (v_default -> 'footer') || CASE WHEN jsonb_typeof(v_legacy -> 'footer') = 'object' THEN v_legacy -> 'footer' ELSE '{}'::jsonb END,
        'contacts', (v_default -> 'contacts') || CASE WHEN jsonb_typeof(v_legacy -> 'contacts') = 'object' THEN v_legacy -> 'contacts' ELSE '{}'::jsonb END,
        'courses', v_courses
    );

    UPDATE landing_content
    SET content = v_result::text
    WHERE id = 1;
END
$migration$;
