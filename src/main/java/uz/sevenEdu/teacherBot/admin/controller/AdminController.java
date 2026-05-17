package uz.sevenEdu.teacherBot.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.entity.Books;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;
import uz.sevenEdu.teacherBot.chat.entity.CourseTeacher;
import uz.sevenEdu.teacherBot.chat.repository.CourseTeacherRepository;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
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

    @GetMapping("/users")
    public Mono<ApiResponse<List<BaseUser>>> getUsers(Authentication auth, @RequestParam(required = false) String role) {
        return requireAdmin(auth).then(
            (role != null ? userRepository.findByRole(role) : userRepository.findAll())
                .collectList()
                .map(ApiResponse::ok)
        );
    }

    @PostMapping("/users")
    public Mono<ApiResponse<BaseUser>> createUser(Authentication auth, @RequestBody Map<String, String> body) {
        return requireAdmin(auth).then(Mono.defer(() -> {
            BaseUser user = BaseUser.builder()
                    .firstName(body.get("firstName"))
                    .lastName(body.get("lastName"))
                    .email(body.get("email"))
                    .phone(body.get("phone"))
                    .password(passwordEncoder.encode(body.getOrDefault("password", "password")))
                    .role(UserRole.valueOf(body.getOrDefault("role", "STUDENT")))
                    .specialization(body.get("specialization"))
                    .ball(0L)
                    .createdAt(LocalDateTime.now())
                    .build();
            return userRepository.save(user).map(ApiResponse::ok);
        }));
    }

    @PutMapping("/users/{id}")
    public Mono<ApiResponse<BaseUser>> updateUser(Authentication auth, @PathVariable Long id, @RequestBody Map<String, String> body) {
        return requireAdmin(auth).then(
            userRepository.findById(id)
                .flatMap(user -> {
                    if (body.containsKey("firstName")) user.setFirstName(body.get("firstName"));
                    if (body.containsKey("lastName")) user.setLastName(body.get("lastName"));
                    if (body.containsKey("email")) user.setEmail(body.get("email"));
                    if (body.containsKey("phone")) user.setPhone(body.get("phone"));
                    if (body.containsKey("role")) user.setRole(UserRole.valueOf(body.get("role")));
                    if (body.containsKey("specialization")) user.setSpecialization(body.get("specialization"));
                    return userRepository.save(user);
                })
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/users/{id}")
    public Mono<ApiResponse<Void>> deleteUser(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            userRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Languages CRUD ──────────────────────────────────────────

    @GetMapping("/languages")
    public Mono<ApiResponse<List<Language>>> getLanguages(Authentication auth) {
        return requireAdmin(auth).then(
            languageRepository.findAll().collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/languages")
    public Mono<ApiResponse<Language>> createLanguage(Authentication auth, @RequestBody Language lang) {
        return requireAdmin(auth).then(
            languageRepository.save(lang).map(ApiResponse::ok)
        );
    }

    @PutMapping("/languages/{id}")
    public Mono<ApiResponse<Language>> updateLanguage(Authentication auth, @PathVariable Long id, @RequestBody Language body) {
        return requireAdmin(auth).then(
            languageRepository.findById(id)
                .flatMap(lang -> {
                    lang.setName(body.getName());
                    lang.setDescription(body.getDescription());
                    lang.setColorStart(body.getColorStart());
                    lang.setColorEnd(body.getColorEnd());
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
                    b.setPages(body.getPages());
                    b.setFormat(body.getFormat());
                    b.setLanguage(body.getLanguage());
                    b.setLevel(body.getLevel());
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
}
