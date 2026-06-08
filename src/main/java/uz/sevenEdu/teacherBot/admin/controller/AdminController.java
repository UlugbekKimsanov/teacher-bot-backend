package uz.sevenEdu.teacherBot.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.entity.Books;
import uz.sevenEdu.teacherBot.books.entity.SaleRecord;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;
import uz.sevenEdu.teacherBot.books.repository.SaleRecordRepository;
import uz.sevenEdu.teacherBot.chat.entity.CourseTeacher;
import uz.sevenEdu.teacherBot.chat.repository.CourseTeacherRepository;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;
import uz.sevenEdu.teacherBot.news.entity.News;
import uz.sevenEdu.teacherBot.news.repository.NewsRepository;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.entity.Language;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.LanguageRepository;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.lesson.entity.*;
import uz.sevenEdu.teacherBot.lesson.repository.*;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LanguageRepository languageRepository;
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final QuestionRepository questionRepository;
    private final ExerciseRepository exerciseRepository;
    private final TestRepository testRepository;
    private final BooksRepository booksRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserCourseRepository userCourseRepository;
    private final FileStorageService fileStorageService;
    private final NewsRepository newsRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Dashboard ──────────────────────────────────────────────

    @GetMapping("/dashboard")
    public Mono<ApiResponse<Map<String, Object>>> dashboard(Authentication auth) {
        return requireAdmin(auth).then(
            Mono.zip(
                userRepository.findByRole("STUDENT").count(),
                userRepository.findByRole("TEACHER").count(),
                courseRepository.findAll().count(),
                lessonRepository.findAll().count(),
                booksRepository.findAll().count()
            ).map(t -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalStudents", t.getT1());
                stats.put("totalTeachers", t.getT2());
                stats.put("totalCourses", t.getT3());
                stats.put("totalLessons", t.getT4());
                stats.put("totalBooks", t.getT5());
                stats.put("activeStudentsToday", 0);
                stats.put("revenue", 0);
                return ApiResponse.ok(stats);
            })
        );
    }

    // ── Users CRUD ─────────────────────────────────────────────

    /**
     * Foydalanuvchilar ro'yxati.
     * isDefault=true admin: barcha userlarni ko'radi (o'zidan tashqari), rol filtri bilan.
     * isDefault=false admin: faqat TEACHER va STUDENT ko'radi, rol filtri bilan.
     */
    @GetMapping("/users")
    public Mono<ApiResponse<List<BaseUser>>> getUsers(Authentication auth, @RequestParam(required = false) String role) {
        return getAdminUser(auth).flatMap(admin -> {
            boolean isDefault = Boolean.TRUE.equals(admin.getIsDefault());
            if (isDefault) {
                // Bosh admin: barchani ko'radi (o'zidan tashqari)
                return (role != null
                        ? userRepository.findByRoleExcept(role, admin.getId())
                        : userRepository.findAllExcept(admin.getId()))
                    .collectList().map(ApiResponse::ok);
            } else {
                // Oddiy admin: faqat TEACHER va STUDENT
                if (role != null && (role.equals("ADMIN") || role.equals("HEAD_ADMIN"))) {
                    return Mono.just(ApiResponse.ok(List.<BaseUser>of()));
                }
                return (role != null
                        ? userRepository.findTeachersAndStudentsByRole(role)
                        : userRepository.findTeachersAndStudents())
                    .collectList().map(ApiResponse::ok);
            }
        });
    }

    /**
     * Yangi foydalanuvchi yaratish.
     * Admin yangi admin yaratsa, isDefault=false bo'ladi.
     */
    @PostMapping("/users")
    public Mono<ApiResponse<BaseUser>> createUser(Authentication auth, @RequestBody Map<String, String> body) {
        return getAdminUser(auth).flatMap(admin -> {
            UserRole newRole = UserRole.valueOf(body.getOrDefault("role", "STUDENT"));

            // isDefault=false admin faqat TEACHER va STUDENT yarata oladi
            boolean isDefault = Boolean.TRUE.equals(admin.getIsDefault());
            if (!isDefault && (newRole == UserRole.ADMIN || newRole == UserRole.HEAD_ADMIN)) {
                return Mono.error(new RuntimeException("Sizda admin yaratish huquqi yo'q"));
            }

            BaseUser user = BaseUser.builder()
                    .firstName(body.get("firstName"))
                    .lastName(body.get("lastName"))
                    .email(body.get("email"))
                    .phone(body.get("phone"))
                    .password(passwordEncoder.encode(body.getOrDefault("password", "password")))
                    .role(newRole)
                    .specialization(body.get("specialization"))
                    .isDefault(false) // yangi admin har doim isDefault=false
                    .ball(0L)
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(user).map(ApiResponse::ok);
        });
    }

    /**
     * Foydalanuvchini tahrirlash.
     * isDefault=true admin: barcha userlarni tahrirlashi mumkin (o'zidan tashqari).
     * isDefault=false admin: faqat TEACHER va STUDENT ni tahrirlashi mumkin.
     */
    @PutMapping("/users/{id}")
    public Mono<ApiResponse<BaseUser>> updateUser(Authentication auth, @PathVariable Long id, @RequestBody Map<String, String> body) {
        return getAdminUser(auth).flatMap(admin -> {
            boolean isDefault = Boolean.TRUE.equals(admin.getIsDefault());

            // O'zini tahrirlashga ruxsat yo'q
            if (admin.getId().equals(id)) {
                return Mono.error(new RuntimeException("O'zingizni tahrirlash mumkin emas"));
            }

            return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Foydalanuvchi topilmadi")))
                .flatMap(user -> {
                    // isDefault=false admin faqat teacher/student tahrirlashi mumkin
                    if (!isDefault && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.HEAD_ADMIN)) {
                        return Mono.error(new RuntimeException("Sizda admin tahrirlash huquqi yo'q"));
                    }

                    if (body.containsKey("firstName")) user.setFirstName(body.get("firstName"));
                    if (body.containsKey("lastName")) user.setLastName(body.get("lastName"));
                    if (body.containsKey("email")) user.setEmail(body.get("email"));
                    if (body.containsKey("phone")) user.setPhone(body.get("phone"));
                    if (body.containsKey("specialization")) user.setSpecialization(body.get("specialization"));

                    // Rolni o'zgartirish
                    if (body.containsKey("role")) {
                        UserRole newRole = UserRole.valueOf(body.get("role"));
                        if (!isDefault && (newRole == UserRole.ADMIN || newRole == UserRole.HEAD_ADMIN)) {
                            return Mono.error(new RuntimeException("Sizda admin roli berish huquqi yo'q"));
                        }
                        user.setRole(newRole);
                    }

                    return userRepository.save(user);
                })
                .map(ApiResponse::ok);
        });
    }

    /**
     * Foydalanuvchini o'chirish.
     * isDefault=true admin: barcha userlarni o'chirishi mumkin (o'zidan tashqari).
     * isDefault=false admin: faqat TEACHER va STUDENT ni o'chirishi mumkin.
     */
    @DeleteMapping("/users/{id}")
    public Mono<ApiResponse<Void>> deleteUser(Authentication auth, @PathVariable Long id) {
        return getAdminUser(auth).flatMap(admin -> {
            boolean isDefault = Boolean.TRUE.equals(admin.getIsDefault());

            if (admin.getId().equals(id)) {
                return Mono.error(new RuntimeException("O'zingizni o'chirish mumkin emas"));
            }

            return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Foydalanuvchi topilmadi")))
                .flatMap(user -> {
                    if (!isDefault && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.HEAD_ADMIN)) {
                        return Mono.error(new RuntimeException("Sizda admin o'chirish huquqi yo'q"));
                    }
                    return userRepository.deleteById(id)
                            .then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)));
                });
        });
    }

    // ── Languages CRUD ──────────────────────────────────────────

    /** Oldindan belgilangan tillar ro'yxati — admin tanlab yaratishi uchun */
    @GetMapping("/languages/predefined")
    public Mono<ApiResponse<List<Map<String, String>>>> getPredefinedLanguages(Authentication auth) {
        return requireAdmin(auth).thenReturn(ApiResponse.ok(PREDEFINED_LANGUAGES));
    }

    @GetMapping("/languages")
    public Mono<ApiResponse<List<Language>>> getLanguages(Authentication auth) {
        return requireAdmin(auth).then(
            languageRepository.findAll().collectList().map(ApiResponse::ok)
        );
    }

    /**
     * Til yaratish. flagEmoji va ranglar oldindan belgilangan ro'yxatdan avtomatik to'ldiriladi.
     * Agar body da flagEmoji, colorStart, colorEnd bo'lmasa — predefined dan olinadi.
     */
    @PostMapping("/languages")
    public Mono<ApiResponse<Language>> createLanguage(Authentication auth, @RequestBody Language lang) {
        return requireAdmin(auth).then(Mono.defer(() -> {
            // Agar flagEmoji/ranglar berilmagan bo'lsa, predefined dan topib to'ldirish
            if (lang.getName() != null) {
                String nameLower = lang.getName().toLowerCase().trim();
                for (var p : PREDEFINED_LANGUAGES) {
                    if (p.get("name").equalsIgnoreCase(lang.getName()) ||
                        nameLower.contains(p.get("keywords").split(",")[0])) {
                        if (lang.getFlagEmoji() == null) lang.setFlagEmoji(p.get("flagEmoji"));
                        if (lang.getColorStart() == null) lang.setColorStart(p.get("colorStart"));
                        if (lang.getColorEnd() == null) lang.setColorEnd(p.get("colorEnd"));
                        break;
                    }
                }
            }
            return languageRepository.save(lang).map(ApiResponse::ok);
        }));
    }

    @PutMapping("/languages/{id}")
    public Mono<ApiResponse<Language>> updateLanguage(Authentication auth, @PathVariable Long id, @RequestBody Language body) {
        return requireAdmin(auth).then(
            languageRepository.findById(id)
                .flatMap(lang -> {
                    if (body.getName() != null) lang.setName(body.getName());
                    if (body.getDescription() != null) lang.setDescription(body.getDescription());
                    if (body.getFlagEmoji() != null) lang.setFlagEmoji(body.getFlagEmoji());
                    if (body.getColorStart() != null) lang.setColorStart(body.getColorStart());
                    if (body.getColorEnd() != null) lang.setColorEnd(body.getColorEnd());
                    return languageRepository.save(lang);
                })
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/languages/{id}")
    public Mono<ApiResponse<Void>> deleteLanguage(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            languageRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Courses CRUD ────────────────────────────────────────────

    @GetMapping("/courses")
    public Mono<ApiResponse<List<Course>>> getCourses(Authentication auth) {
        return requireAdmin(auth).then(
            courseRepository.findAll().collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/courses")
    public Mono<ApiResponse<Course>> createCourse(Authentication auth, @RequestBody Course course) {
        return requireAdmin(auth).then(
            courseRepository.save(course).map(ApiResponse::ok)
        );
    }

    @PutMapping("/courses/{id}")
    public Mono<ApiResponse<Course>> updateCourse(Authentication auth, @PathVariable Long id, @RequestBody Course body) {
        return requireAdmin(auth).then(
            courseRepository.findById(id)
                .flatMap(c -> {
                    c.setName(body.getName());
                    c.setLanguageId(body.getLanguageId());
                    c.setFlagEmoji(body.getFlagEmoji());
                    c.setGoal(body.getGoal());
                    c.setIsPremium(body.getIsPremium());
                    c.setCoverImage(body.getCoverImage());
                    c.setImageId(body.getImageId());
                    return courseRepository.save(c);
                })
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/courses/{id}")
    public Mono<ApiResponse<Void>> deleteCourse(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            courseRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Course Teachers ─────────────────────────────────────────

    @GetMapping("/courses/{courseId}/teachers")
    public Mono<ApiResponse<List<BaseUser>>> getCourseTeachers(Authentication auth, @PathVariable Long courseId) {
        return requireAdmin(auth).then(
            courseTeacherRepository.findByCourseId(courseId)
                .flatMap(ct -> userRepository.findById(ct.getTeacherId()))
                .collectList()
                .map(ApiResponse::ok)
        );
    }

    @PostMapping("/courses/{courseId}/teachers/{teacherId}")
    public Mono<ApiResponse<CourseTeacher>> addTeacherToCourse(Authentication auth, @PathVariable Long courseId, @PathVariable Long teacherId) {
        return requireAdmin(auth).then(
            courseTeacherRepository.save(CourseTeacher.builder().courseId(courseId).teacherId(teacherId).build())
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/courses/{courseId}/teachers/{teacherId}")
    public Mono<ApiResponse<Void>> removeTeacherFromCourse(Authentication auth, @PathVariable Long courseId, @PathVariable Long teacherId) {
        return requireAdmin(auth).then(
            courseTeacherRepository.findByCourseIdAndTeacherId(courseId, teacherId)
                .flatMap(ct -> courseTeacherRepository.delete(ct))
                .then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Lessons CRUD ────────────────────────────────────────────

    @GetMapping("/courses/{courseId}/lessons")
    public Mono<ApiResponse<List<Lesson>>> getLessons(Authentication auth, @PathVariable Long courseId) {
        return requireAdmin(auth).then(
            lessonRepository.findByCourseIdOrderByOrderIndex(courseId).collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/lessons")
    public Mono<ApiResponse<Lesson>> createLesson(Authentication auth, @RequestBody Lesson lesson) {
        return requireAdmin(auth).then(Mono.defer(() -> {
            Lesson l = Lesson.builder()
                    .courseId(lesson.getCourseId())
                    .name(lesson.getName())
                    .description(lesson.getDescription())
                    .orderIndex(lesson.getOrderIndex())
                    .durationSec(lesson.getDurationSec())
                    .coverImage(lesson.getCoverImage())
                    .videoUrl(lesson.getVideoUrl())
                    .build();
            return lessonRepository.save(l)
                    .flatMap(saved ->
                        testRepository.save(TestEntity.builder().lessonId(saved.getId()).name(saved.getName() + " Test").build())
                            .thenReturn(saved)
                    )
                    .map(ApiResponse::ok);
        }));
    }

    @PutMapping("/lessons/{id}")
    public Mono<ApiResponse<Lesson>> updateLesson(Authentication auth, @PathVariable Long id, @RequestBody Lesson body) {
        return requireAdmin(auth).then(
            lessonRepository.findById(id)
                .flatMap(l -> {
                    l.setName(body.getName());
                    l.setDescription(body.getDescription());
                    l.setOrderIndex(body.getOrderIndex());
                    l.setDurationSec(body.getDurationSec());
                    l.setCoverImage(body.getCoverImage());
                    l.setVideoUrl(body.getVideoUrl());
                    return lessonRepository.save(l);
                })
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/lessons/{id}")
    public Mono<ApiResponse<Void>> deleteLesson(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            lessonRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Vocabulary CRUD ──────────────────────────────────────────

    @GetMapping("/lessons/{lessonId}/vocabulary")
    public Mono<ApiResponse<List<Vocabulary>>> getVocabulary(Authentication auth, @PathVariable Long lessonId) {
        return requireAdmin(auth).then(
            vocabularyRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/lessons/{lessonId}/vocabulary")
    public Mono<ApiResponse<Vocabulary>> createVocab(Authentication auth, @PathVariable Long lessonId, @RequestBody Vocabulary vocab) {
        vocab.setLessonId(lessonId);
        return requireAdmin(auth).then(
            vocabularyRepository.save(vocab).map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/vocabulary/{id}")
    public Mono<ApiResponse<Void>> deleteVocab(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            vocabularyRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Questions CRUD ───────────────────────────────────────────

    @GetMapping("/lessons/{lessonId}/questions")
    public Mono<ApiResponse<List<Question>>> getQuestions(Authentication auth, @PathVariable Long lessonId) {
        return requireAdmin(auth).then(
            questionRepository.findByLessonId(lessonId).collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/lessons/{lessonId}/questions")
    public Mono<ApiResponse<Question>> createQuestion(Authentication auth, @PathVariable Long lessonId, @RequestBody Question q) {
        return requireAdmin(auth).then(
            testRepository.findByLessonId(lessonId)
                .flatMap(test -> {
                    q.setTestId(test.getId());
                    return questionRepository.save(q);
                })
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/questions/{id}")
    public Mono<ApiResponse<Void>> deleteQuestion(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            questionRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Exercises CRUD ───────────────────────────────────────────

    @GetMapping("/lessons/{lessonId}/exercises")
    public Mono<ApiResponse<List<Exercise>>> getExercises(Authentication auth, @PathVariable Long lessonId) {
        return requireAdmin(auth).then(
            exerciseRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/lessons/{lessonId}/exercises")
    public Mono<ApiResponse<Exercise>> createExercise(Authentication auth, @PathVariable Long lessonId, @RequestBody Exercise ex) {
        ex.setLessonId(lessonId);
        return requireAdmin(auth).then(
            exerciseRepository.save(ex).map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/exercises/{id}")
    public Mono<ApiResponse<Void>> deleteExercise(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            exerciseRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Books CRUD ───────────────────────────────────────────────

    @GetMapping("/books")
    public Mono<ApiResponse<List<Books>>> getBooks(Authentication auth) {
        return requireAdmin(auth).then(
            booksRepository.findAll().collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/books")
    public Mono<ApiResponse<Books>> createBook(Authentication auth, @RequestBody Books book) {
        return requireAdmin(auth).then(
            booksRepository.save(book).map(ApiResponse::ok)
        );
    }

    @PutMapping("/books/{id}")
    public Mono<ApiResponse<Books>> updateBook(Authentication auth, @PathVariable Long id, @RequestBody Books body) {
        return requireAdmin(auth).then(
            booksRepository.findById(id)
                .flatMap(b -> {
                    b.setTitle(body.getTitle());
                    b.setAuthor(body.getAuthor());
                    b.setCategory(body.getCategory());
                    b.setDescription(body.getDescription());
                    b.setPrice(body.getPrice());
                    b.setPriceLabel(body.getPriceLabel());
                    b.setIsFree(body.getIsFree());
                    b.setEmoji(body.getEmoji());
                    b.setCoverColor1(body.getCoverColor1());
                    b.setCoverColor2(body.getCoverColor2());
                    b.setPages(body.getPages());
                    b.setPageCount(body.getPageCount());
                    b.setFormat(body.getFormat());
                    b.setRating(body.getRating());
                    b.setReviewCount(body.getReviewCount());
                    b.setLanguage(body.getLanguage());
                    b.setLevel(body.getLevel());
                    b.setPreviewPages(body.getPreviewPages());
                    b.setImageId(body.getImageId());
                    b.setFileId(body.getFileId());
                    b.setFilePath(body.getFilePath());
                    b.setUpdatedAt(LocalDateTime.now());
                    return booksRepository.save(b);
                })
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/books/{id}")
    public Mono<ApiResponse<Void>> deleteBook(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            booksRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    /**
     * Kitob PDF/EPUB faylini yuklash
     */
    @PostMapping(value = "/books/{id}/upload-file", consumes = "multipart/form-data")
    public Mono<ApiResponse<Books>> uploadBookFile(Authentication auth, @PathVariable Long id,
                                                    @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(book -> fileStorageService.saveBookFile(id, file, book.getFilePath())
                    .flatMap(path -> {
                        book.setFilePath(path);
                        book.setUpdatedAt(LocalDateTime.now());
                        return booksRepository.save(book);
                    })
                )
                .map(ApiResponse::ok)
        );
    }

    /**
     * Kitob muqova rasmini yuklash
     */
    @PostMapping(value = "/books/{id}/upload-cover", consumes = "multipart/form-data")
    public Mono<ApiResponse<Books>> uploadBookCover(Authentication auth, @PathVariable Long id,
                                                     @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(book -> fileStorageService.saveBookCover(id, file, null)
                    .flatMap(path -> {
                        // coverImage sifatida saqlash (public URL)
                        book.setUpdatedAt(LocalDateTime.now());
                        return booksRepository.save(book);
                    })
                )
                .map(ApiResponse::ok)
        );
    }

    // ── News CRUD ───────────────────────────────────────────────

    @GetMapping("/news")
    public Mono<ApiResponse<List<News>>> getNews(Authentication auth) {
        return requireAdmin(auth).then(
            newsRepository.findAll().collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/news")
    public Mono<ApiResponse<News>> createNews(Authentication auth, @RequestBody News news) {
        news.setCreatedAt(LocalDateTime.now());
        return requireAdmin(auth).then(
            newsRepository.save(news).map(ApiResponse::ok)
        );
    }

    @PutMapping("/news/{id}")
    public Mono<ApiResponse<News>> updateNews(Authentication auth, @PathVariable Long id, @RequestBody News body) {
        return requireAdmin(auth).then(
            newsRepository.findById(id)
                .flatMap(n -> {
                    n.setName(body.getName());
                    n.setImageId(body.getImageId());
                    return newsRepository.save(n);
                })
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/news/{id}")
    public Mono<ApiResponse<Void>> deleteNews(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            newsRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Sales (sotuv statistikasi) ──────────────────────────────

    /**
     * Barcha sotuv qaydlari ro'yxati (yangi → eski)
     */
    @GetMapping("/sales")
    public Mono<ApiResponse<List<SaleRecord>>> getSales(Authentication auth) {
        return requireAdmin(auth).then(
            saleRecordRepository.findAllByOrderByCreatedAtDesc()
                .collectList().map(ApiResponse::ok)
        );
    }

    /**
     * Sotuv statistikasi: jami daromad, sotuvlar soni, to'lov usuli bo'yicha
     */
    @GetMapping("/sales/stats")
    public Mono<ApiResponse<Map<String, Object>>> getSalesStats(Authentication auth) {
        return requireAdmin(auth).then(
            saleRecordRepository.findAllByOrderByCreatedAtDesc().collectList()
                .map(sales -> {
                    Map<String, Object> stats = new HashMap<>();
                    long totalRevenue = sales.stream()
                            .mapToLong(s -> s.getAmount() != null ? s.getAmount() : 0).sum();
                    stats.put("totalRevenue", totalRevenue);
                    stats.put("totalSales", sales.size());

                    // To'lov usuli bo'yicha guruhlash
                    Map<String, List<SaleRecord>> grouped = sales.stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    s -> s.getPaymentMethod() != null ? s.getPaymentMethod() : "unknown"));
                    List<Map<String, Object>> byMethod = grouped.entrySet().stream().map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("paymentMethod", e.getKey());
                        m.put("count", e.getValue().size());
                        m.put("totalAmount", e.getValue().stream()
                                .mapToLong(s -> s.getAmount() != null ? s.getAmount() : 0).sum());
                        return m;
                    }).toList();
                    stats.put("byPaymentMethod", byMethod);

                    return ApiResponse.ok(stats);
                })
        );
    }

    // ── Helper ──────────────────────────────────────────────────

    private Mono<Void> requireAdmin(Authentication auth) {
        if (auth == null) return Mono.error(new RuntimeException("Unauthorized"));
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .flatMap(user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        return Mono.error(new RuntimeException("Faqat admin uchun"));
                    }
                    return Mono.empty();
                });
    }

    // ── Predefined Languages ──────────────────────────────────────────────────

    private static final List<Map<String, String>> PREDEFINED_LANGUAGES = List.of(
        Map.of("name","Ingliz tili",  "flagEmoji","🇬🇧", "colorStart","#4FC3F7", "colorEnd","#1565C0", "keywords","english,ingliz,british"),
        Map.of("name","Ingliz tili (AQSh)", "flagEmoji","🇺🇸", "colorStart","#64B5F6", "colorEnd","#1E88E5", "keywords","american,amerikancha"),
        Map.of("name","O'zbek tili",  "flagEmoji","🇺🇿", "colorStart","#66BB6A", "colorEnd","#1B5E20", "keywords","uzbek,o'zbek"),
        Map.of("name","Rus tili",     "flagEmoji","🇷🇺", "colorStart","#EF5350", "colorEnd","#6A0000", "keywords","russian,ruscha,rus"),
        Map.of("name","Nemis tili",   "flagEmoji","🇩🇪", "colorStart","#F2994A", "colorEnd","#6D4C41", "keywords","german,deutsch,nemis"),
        Map.of("name","Koreys tili",  "flagEmoji","🇰🇷", "colorStart","#A770EF", "colorEnd","#4A148C", "keywords","korean,koreys"),
        Map.of("name","Turk tili",    "flagEmoji","🇹🇷", "colorStart","#E44D26", "colorEnd","#6D0000", "keywords","turkish,turkcha,turk"),
        Map.of("name","Arab tili",    "flagEmoji","🇸🇦", "colorStart","#26A69A", "colorEnd","#00695C", "keywords","arabic,arabcha,arab"),
        Map.of("name","Fransuz tili", "flagEmoji","🇫🇷", "colorStart","#5C6BC0", "colorEnd","#1A237E", "keywords","french,fransuz"),
        Map.of("name","Xitoy tili",   "flagEmoji","🇨🇳", "colorStart","#EF5350", "colorEnd","#7F0000", "keywords","chinese,xitoy,mandarin"),
        Map.of("name","Ispan tili",   "flagEmoji","🇪🇸", "colorStart","#FF7043", "colorEnd","#BF360C", "keywords","spanish,ispan"),
        Map.of("name","Italyan tili", "flagEmoji","🇮🇹", "colorStart","#388E3C", "colorEnd","#B71C1C", "keywords","italian,italyan"),
        Map.of("name","Yapon tili",   "flagEmoji","🇯🇵", "colorStart","#EF5350", "colorEnd","#AD1457", "keywords","japanese,yapon"),
        Map.of("name","Portugal tili","flagEmoji","🇵🇹", "colorStart","#388E3C", "colorEnd","#1B5E20", "keywords","portuguese,portugal"),
        Map.of("name","Hind tili",    "flagEmoji","🇮🇳", "colorStart","#FF7043", "colorEnd","#6D4C41", "keywords","hindi,hindcha"),
        Map.of("name","Fors tili",    "flagEmoji","🇮🇷", "colorStart","#388E3C", "colorEnd","#006064", "keywords","persian,farsi,forscha"),
        Map.of("name","Tojik tili",   "flagEmoji","🇹🇯", "colorStart","#26C6DA", "colorEnd","#006064", "keywords","tajik,tojik"),
        Map.of("name","Qozoq tili",   "flagEmoji","🇰🇿", "colorStart","#26A69A", "colorEnd","#004D40", "keywords","kazakh,qozoq"),
        Map.of("name","Qirg'iz tili", "flagEmoji","🇰🇬", "colorStart","#EF5350", "colorEnd","#E65100", "keywords","kyrgyz,qirg'iz"),
        Map.of("name","Ukraina tili", "flagEmoji","🇺🇦", "colorStart","#42A5F5", "colorEnd","#FDD835", "keywords","ukrainian,ukrain"),
        Map.of("name","Polyak tili",  "flagEmoji","🇵🇱", "colorStart","#EF5350", "colorEnd","#6A0000", "keywords","polish,polyak"),
        Map.of("name","Golland tili", "flagEmoji","🇳🇱", "colorStart","#1565C0", "colorEnd","#E53935", "keywords","dutch,golland"),
        Map.of("name","Shved tili",   "flagEmoji","🇸🇪", "colorStart","#1565C0", "colorEnd","#E65100", "keywords","swedish,shved"),
        Map.of("name","Turkman tili", "flagEmoji","🇹🇲", "colorStart","#388E3C", "colorEnd","#26A69A", "keywords","turkmen,turkman"),
        Map.of("name","Ozarbayjon tili","flagEmoji","🇦🇿","colorStart","#26C6DA", "colorEnd","#1B5E20", "keywords","azerbaijani,ozarbayjon"),
        Map.of("name","Gruzin tili",  "flagEmoji","🇬🇪", "colorStart","#EF5350", "colorEnd","#880E4F", "keywords","georgian,gruzin"),
        Map.of("name","Arman tili",   "flagEmoji","🇦🇲", "colorStart","#EF5350", "colorEnd","#1565C0", "keywords","armenian,arman")
    );

    /** Admin userni qaytaradi (requireAdmin + user ma'lumotlari) */
    private Mono<BaseUser> getAdminUser(Authentication auth) {
        if (auth == null) return Mono.error(new RuntimeException("Unauthorized"));
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new RuntimeException("Foydalanuvchi topilmadi")))
                .flatMap(user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        return Mono.error(new RuntimeException("Faqat admin uchun"));
                    }
                    return Mono.just(user);
                });
    }
}
