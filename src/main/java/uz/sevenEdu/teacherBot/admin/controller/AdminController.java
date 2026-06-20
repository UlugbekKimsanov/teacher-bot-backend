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
    private final uz.sevenEdu.teacherBot.course.service.LanguageService languageService;
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
    private final uz.sevenEdu.teacherBot.payment.repository.PaymentMethodRepository paymentMethodRepository;
    private final uz.sevenEdu.teacherBot.payment.order.repository.PaymentOrderRepository paymentOrderRepository;
    private final uz.sevenEdu.teacherBot.rating.repository.AttendanceRepository attendanceRepository;
    private final uz.sevenEdu.teacherBot.notification.service.NotificationService notificationService;
    private final uz.sevenEdu.teacherBot.breakmusic.repository.BreakGroupRepository breakGroupRepository;
    private final uz.sevenEdu.teacherBot.breakmusic.repository.BreakTrackRepository breakTrackRepository;
    private final uz.sevenEdu.teacherBot.lesson.repository.AudiobookRepository audiobookRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Dashboard ──────────────────────────────────────────────

    @GetMapping("/dashboard")
    public Mono<ApiResponse<Map<String, Object>>> dashboard(Authentication auth) {
        return requireAdmin(auth).then(
            Mono.zip(
                userRepository.countRealStudents(),
                userRepository.findByRole("TEACHER").count(),
                courseRepository.findAll().count(),
                lessonRepository.findAll().count(),
                booksRepository.findAll().count(),
                saleRecordRepository.totalRevenue(),       // kitob savdosi (so'm)
                paymentOrderRepository.totalPaidAmount(),   // to'langan buyurtmalar (so'm)
                attendanceRepository.countActiveToday()     // bugun faol o'quvchilar
            ).map(t -> {
                long revenue = t.getT6() + t.getT7().longValue();
                Map<String, Object> stats = new HashMap<>();
                stats.put("totalStudents", t.getT1());
                stats.put("totalTeachers", t.getT2());
                stats.put("totalCourses", t.getT3());
                stats.put("totalLessons", t.getT4());
                stats.put("totalBooks", t.getT5());
                stats.put("activeStudentsToday", t.getT8());
                stats.put("revenue", revenue);
                return ApiResponse.ok(stats);
            })
        );
    }

    /** Dashboard "so'nggi faoliyat" — oxirgi ro'yxatdan o'tganlar + kitob savdolari. */
    @GetMapping("/recent-activity")
    public Mono<ApiResponse<List<Map<String, Object>>>> recentActivity(Authentication auth) {
        return requireAdmin(auth).then(
            Mono.zip(
                userRepository.findRecentByRole("STUDENT").collectList(),
                saleRecordRepository.findAllByOrderByCreatedAtDesc().take(6).collectList()
            ).map(t -> {
                List<Map<String, Object>> items = new java.util.ArrayList<>();
                t.getT1().forEach(u -> {
                    String fn = u.getFirstName() != null ? u.getFirstName() : "";
                    String ln = u.getLastName() != null ? u.getLastName() : "";
                    String name = (fn + " " + ln).trim();
                    if (name.isEmpty()) name = u.getEmail() != null ? u.getEmail() : "Foydalanuvchi";
                    Map<String, Object> m = new HashMap<>();
                    m.put("text", "Yangi o'quvchi ro'yxatdan o'tdi: " + name);
                    m.put("_at", u.getCreatedAt());
                    items.add(m);
                });
                t.getT2().forEach(s -> {
                    String buyer = s.getBuyerName() != null ? " — " + s.getBuyerName() : "";
                    Map<String, Object> m = new HashMap<>();
                    m.put("text", "Kitob sotib olindi: " + s.getBookTitle() + buyer);
                    m.put("_at", s.getCreatedAt());
                    items.add(m);
                });
                items.sort((a, b) -> {
                    java.time.LocalDateTime ta = (java.time.LocalDateTime) a.get("_at");
                    java.time.LocalDateTime tb = (java.time.LocalDateTime) b.get("_at");
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                });
                List<Map<String, Object>> result = new java.util.ArrayList<>();
                items.stream().limit(8).forEach(m -> {
                    java.time.LocalDateTime at = (java.time.LocalDateTime) m.get("_at");
                    Map<String, Object> out = new HashMap<>();
                    out.put("text", m.get("text"));
                    out.put("createdAt", at != null ? at.toString() : null);
                    result.add(out);
                });
                return ApiResponse.ok(result);
            })
        );
    }

    /** Daromad ro'yxati — har bir to'lov to'liq detali bilan (yangidan eskiga). */
    @GetMapping("/income")
    public Mono<ApiResponse<List<Map<String, Object>>>> income(Authentication auth) {
        return requireAdmin(auth).then(
            Mono.zip(
                saleRecordRepository.findAllByOrderByCreatedAtDesc().map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("who", s.getBuyerName() != null ? s.getBuyerName() : "Noma'lum");
                    m.put("operator", s.getPaymentMethod());
                    m.put("product", s.getBookTitle());
                    m.put("productType", "BOOK");
                    m.put("amount", s.getAmount());
                    m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
                    m.put("_at", s.getCreatedAt());
                    return m;
                }).collectList(),
                paymentOrderRepository.findPaidOrders().flatMap(o -> {
                    Mono<String> whoMono = userRepository.findById(o.getUserId())
                            .map(u -> {
                                String fn = u.getFirstName() != null ? u.getFirstName() : "";
                                String ln = u.getLastName() != null ? u.getLastName() : "";
                                String nm = (fn + " " + ln).trim();
                                return nm.isEmpty() ? (u.getEmail() != null ? u.getEmail() : "Foydalanuvchi") : nm;
                            })
                            .defaultIfEmpty("Noma'lum");
                    Mono<String> productMono = "COURSE".equalsIgnoreCase(o.getProductType())
                            ? courseRepository.findById(o.getProductId()).map(Course::getName).defaultIfEmpty("Kurs #" + o.getProductId())
                            : booksRepository.findById(o.getProductId()).map(Books::getTitle).defaultIfEmpty("Kitob #" + o.getProductId());
                    return Mono.zip(whoMono, productMono).map(tp -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("who", tp.getT1());
                        m.put("operator", o.getPaymentMethod());
                        m.put("product", tp.getT2());
                        m.put("productType", o.getProductType());
                        m.put("amount", o.getAmount() != null ? o.getAmount().longValue() : 0);
                        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                        m.put("_at", o.getCreatedAt());
                        return m;
                    });
                }).collectList()
            ).map(t -> {
                List<Map<String, Object>> all = new java.util.ArrayList<>();
                all.addAll(t.getT1());
                all.addAll(t.getT2());
                all.sort((a, b) -> {
                    java.time.LocalDateTime ta = (java.time.LocalDateTime) a.get("_at");
                    java.time.LocalDateTime tb = (java.time.LocalDateTime) b.get("_at");
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                });
                all.forEach(m -> m.remove("_at"));
                return ApiResponse.ok(all);
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

    // ── Languages (faqat enable/disable) ────────────────────────
    // Eslatma: tillar ro'yxati belgilangan (top 30). Admin yangi til
    // qo'sha olmaydi yoki o'chira olmaydi — faqat enable/disable qiladi.

    @GetMapping("/languages")
    public Mono<ApiResponse<List<Language>>> getLanguages(Authentication auth) {
        return requireAdmin(auth).then(
            languageService.getAllAdmin().collectList().map(ApiResponse::ok)
        );
    }

    /** Tilni yoqish/o'chirish (enable/disable). */
    @PatchMapping("/languages/{id}/status")
    public Mono<ApiResponse<Language>> setLanguageStatus(Authentication auth,
                                                         @PathVariable Long id,
                                                         @RequestParam boolean enabled) {
        return requireAdmin(auth).then(
            languageRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Til topilmadi")))
                .flatMap(lang -> {
                    lang.setEnabled(enabled);
                    return languageRepository.save(lang);
                })
                .map(ApiResponse::ok)
        );
    }

    // ── Tanaffus musiqasi (admin) ───────────────────────────────

    @GetMapping("/break-groups")
    public Mono<ApiResponse<List<uz.sevenEdu.teacherBot.breakmusic.entity.BreakGroup>>> getBreakGroups(Authentication auth) {
        return requireAdmin(auth).then(
            breakGroupRepository.findAllByOrderByOrderIndexAsc()
                .flatMap(group -> breakTrackRepository.findByGroupIdOrderById(group.getId())
                        .map(t -> { t.setFilePath(fileStorageService.toPublicUrl(t.getFilePath())); return t; })
                        .collectList()
                        .map(tracks -> {
                            group.setBackgroundImage(fileStorageService.toPublicUrl(group.getBackgroundImage()));
                            group.setTracks(tracks);
                            return group;
                        }))
                .collectList().map(ApiResponse::ok)
        );
    }

    /** Guruh fon rasmini yuklash. */
    @PostMapping(value = "/break-groups/{id}/upload-bg", consumes = "multipart/form-data")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.breakmusic.entity.BreakGroup>> uploadBreakBg(
            Authentication auth, @PathVariable Long id, @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            breakGroupRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Guruh topilmadi")))
                .flatMap(g -> fileStorageService.saveBreakBackground(id, file, g.getBackgroundImage())
                    .flatMap(path -> { g.setBackgroundImage(path); return breakGroupRepository.save(g); }))
                .map(ApiResponse::ok)
        );
    }

    /** Guruhga musiqa qo'shish (fayl). */
    @PostMapping(value = "/break-groups/{id}/tracks", consumes = "multipart/form-data")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.breakmusic.entity.BreakTrack>> uploadBreakTrack(
            Authentication auth, @PathVariable Long id,
            @RequestPart("file") FilePart file,
            @RequestParam(required = false) String title) {
        return requireAdmin(auth).then(
            breakGroupRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Guruh topilmadi")))
                .flatMap(g -> fileStorageService.saveBreakTrack(id, file)
                    .flatMap(path -> breakTrackRepository.save(
                            uz.sevenEdu.teacherBot.breakmusic.entity.BreakTrack.builder()
                                    .groupId(id)
                                    .title(title != null && !title.isBlank() ? title : file.filename())
                                    .filePath(path)
                                    .createdAt(java.time.LocalDateTime.now())
                                    .build())))
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/break-tracks/{id}")
    public Mono<ApiResponse<Void>> deleteBreakTrack(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            breakTrackRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    // ── Bildirishnoma yuborish (admin) ──────────────────────────

    /**
     * Admin tomonidan xabar yuborish.
     * target: ALL (barchaga) | STUDENTS | TEACHERS | COURSE | USERS
     * COURSE uchun courseId, USERS uchun userIds majburiy.
     */
    @PostMapping("/notifications")
    public Mono<ApiResponse<Map<String, Object>>> sendNotification(Authentication auth,
                                                                   @RequestBody Map<String, Object> body) {
        return requireAdmin(auth).then(Mono.defer(() -> {
            String target = String.valueOf(body.getOrDefault("target", "ALL")).toUpperCase();
            String title = body.get("title") != null ? body.get("title").toString().trim() : "";
            String text = body.get("body") != null ? body.get("body").toString().trim() : "";
            if (title.isEmpty() || text.isEmpty()) {
                return Mono.error(new uz.sevenEdu.teacherBot.common.exception.BadRequestException("Sarlavha va matn majburiy"));
            }

            reactor.core.publisher.Flux<Long> userIds;
            switch (target) {
                case "STUDENTS" -> userIds = userRepository.findByRole("STUDENT").map(BaseUser::getId);
                case "TEACHERS" -> userIds = userRepository.findByRole("TEACHER").map(BaseUser::getId);
                case "COURSE" -> {
                    Long courseId = body.get("courseId") != null
                            ? Long.valueOf(body.get("courseId").toString()) : null;
                    if (courseId == null) return Mono.error(new uz.sevenEdu.teacherBot.common.exception.BadRequestException("courseId majburiy"));
                    userIds = userCourseRepository.findByCourseId(courseId)
                            .map(uz.sevenEdu.teacherBot.course.entity.UserCourse::getUserId);
                }
                case "USERS" -> {
                    Object raw = body.get("userIds");
                    if (!(raw instanceof List<?> list) || list.isEmpty()) {
                        return Mono.error(new uz.sevenEdu.teacherBot.common.exception.BadRequestException("userIds majburiy"));
                    }
                    java.util.List<Long> ids = list.stream()
                            .map(o -> Long.valueOf(o.toString())).toList();
                    userIds = reactor.core.publisher.Flux.fromIterable(ids);
                }
                default -> userIds = userRepository.findStudentsAndTeachers().map(BaseUser::getId);
            }

            return userIds.collectList()
                    .flatMap(ids -> notificationService.sendBulk(ids, title, text, "ADMIN")
                            .map(sent -> {
                                Map<String, Object> res = new HashMap<>();
                                res.put("sent", sent);
                                return ApiResponse.ok("Yuborildi", res);
                            }));
        }));
    }

    // ── To'lov tizimlari (enable/disable) ───────────────────────

    @GetMapping("/payment-methods")
    public Mono<ApiResponse<List<uz.sevenEdu.teacherBot.payment.entity.PaymentMethod>>> getPaymentMethods(Authentication auth) {
        return requireAdmin(auth).then(
            paymentMethodRepository.findAllByOrderByOrderIndexAsc().collectList().map(ApiResponse::ok)
        );
    }

    @PatchMapping("/payment-methods/{id}/status")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.payment.entity.PaymentMethod>> setPaymentMethodStatus(
            Authentication auth, @PathVariable Long id, @RequestParam boolean enabled) {
        return requireAdmin(auth).then(
            paymentMethodRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("To'lov usuli topilmadi")))
                .flatMap(pm -> {
                    pm.setEnabled(enabled);
                    return paymentMethodRepository.save(pm);
                })
                .map(ApiResponse::ok)
        );
    }

    // ── Courses CRUD ────────────────────────────────────────────

    @GetMapping("/courses")
    public Mono<ApiResponse<List<Course>>> getCourses(Authentication auth) {
        return requireAdmin(auth).then(
            courseRepository.findAll()
                .flatMap(c -> Mono.zip(
                        userCourseRepository.countByCourseId(c.getId()),
                        lessonRepository.countByCourseId(c.getId())
                ).map(t -> {
                    c.setStudentCount(t.getT1());
                    c.setLessonCount(t.getT2());
                    return c;
                }))
                .sort((a, b) -> Integer.compare(
                        a.getOrderIndex() != null ? a.getOrderIndex() : a.getId().intValue(),
                        b.getOrderIndex() != null ? b.getOrderIndex() : b.getId().intValue()))
                .collectList().map(ApiResponse::ok)
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
                    c.setPrice(body.getPrice());
                    c.setPriceLabel(body.getPriceLabel());
                    if (body.getBackgroundImage() != null) c.setBackgroundImage(body.getBackgroundImage());
                    if (body.getOrderIndex() != null) c.setOrderIndex(body.getOrderIndex());
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

    /** Kurs card (cover) rasmini yuklash. */
    @PostMapping(value = "/courses/{id}/upload-cover", consumes = "multipart/form-data")
    public Mono<ApiResponse<Course>> uploadCourseCover(Authentication auth, @PathVariable Long id,
                                                       @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            courseRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kurs topilmadi")))
                .flatMap(course -> fileStorageService.saveCourseCover(id, file, course.getCoverImage())
                    .flatMap(path -> {
                        course.setCoverImage(path);
                        return courseRepository.save(course);
                    }))
                .map(ApiResponse::ok)
        );
    }

    /** Kurs background rasmini yuklash. */
    @PostMapping(value = "/courses/{id}/upload-background", consumes = "multipart/form-data")
    public Mono<ApiResponse<Course>> uploadCourseBackground(Authentication auth, @PathVariable Long id,
                                                            @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            courseRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kurs topilmadi")))
                .flatMap(course -> fileStorageService.saveCourseBackground(id, file, course.getBackgroundImage())
                    .flatMap(path -> {
                        course.setBackgroundImage(path);
                        return courseRepository.save(course);
                    }))
                .map(ApiResponse::ok)
        );
    }

    /** Til sahifasi background rasmini yuklash. */
    @PostMapping(value = "/languages/{id}/upload-background", consumes = "multipart/form-data")
    public Mono<ApiResponse<Language>> uploadLanguageBackground(Authentication auth, @PathVariable Long id,
                                                                @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            languageRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Til topilmadi")))
                .flatMap(lang -> fileStorageService.saveLanguageImage(
                                lang.getName(), lang.getId(),
                                uz.sevenEdu.teacherBot.common.enums.LanguageFileType.BACKGROUND,
                                file, lang.getBackgroundImage())
                    .flatMap(path -> {
                        lang.setBackgroundImage(path);
                        return languageRepository.save(lang);
                    }))
                .map(ApiResponse::ok)
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

    /** Dars videosini yuklash (URL emas, fayl orqali). */
    @PostMapping(value = "/lessons/{id}/upload-video", consumes = "multipart/form-data")
    public Mono<ApiResponse<Lesson>> uploadLessonVideo(Authentication auth, @PathVariable Long id,
                                                       @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            lessonRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Dars topilmadi")))
                .flatMap(lesson -> fileStorageService.saveLessonVideo(id, file, lesson.getVideoUrl())
                    .flatMap(path -> {
                        lesson.setVideoUrl(path);
                        return lessonRepository.save(lesson);
                    }))
                .map(ApiResponse::ok)
        );
    }

    /** Dars muqova (oboloshka) rasmini yuklash — ixtiyoriy. */
    @PostMapping(value = "/lessons/{id}/upload-cover", consumes = "multipart/form-data")
    public Mono<ApiResponse<Lesson>> uploadLessonCover(Authentication auth, @PathVariable Long id,
                                                       @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            lessonRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Dars topilmadi")))
                .flatMap(lesson -> fileStorageService.saveLessonCover(id, file, lesson.getCoverImage())
                    .flatMap(path -> {
                        lesson.setCoverImage(path);
                        return lessonRepository.save(lesson);
                    }))
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

    // ── Audio kitob CRUD ─────────────────────────────────────────

    @GetMapping("/lessons/{lessonId}/audiobooks")
    public Mono<ApiResponse<List<uz.sevenEdu.teacherBot.lesson.entity.Audiobook>>> getAudiobooks(
            Authentication auth, @PathVariable Long lessonId) {
        return requireAdmin(auth).then(
            audiobookRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList().map(ApiResponse::ok)
        );
    }

    /** Darsga audio kitob (audio fayl) qo'shish. */
    @PostMapping(value = "/lessons/{lessonId}/audiobooks", consumes = "multipart/form-data")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.lesson.entity.Audiobook>> uploadAudiobook(
            Authentication auth, @PathVariable Long lessonId,
            @RequestPart("file") FilePart file,
            @RequestParam(required = false) String title) {
        return requireAdmin(auth).then(
            lessonRepository.findById(lessonId)
                .switchIfEmpty(Mono.error(new RuntimeException("Dars topilmadi")))
                .then(audiobookRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList())
                .flatMap(existing -> fileStorageService.saveLessonAudiobook(lessonId, file)
                    .flatMap(path -> audiobookRepository.save(
                            uz.sevenEdu.teacherBot.lesson.entity.Audiobook.builder()
                                    .lessonId(lessonId)
                                    .title(title != null && !title.isBlank() ? title : file.filename())
                                    .filePath(path)
                                    .orderIndex(existing.size() + 1)
                                    .createdAt(java.time.LocalDateTime.now())
                                    .build())))
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/audiobooks/{id}")
    public Mono<ApiResponse<Void>> deleteAudiobook(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            audiobookRepository.deleteById(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
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
                    // format/daraja olib tashlandi; reyting qo'lda emas (baholar asosida)
                    b.setLanguage(body.getLanguage());
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
                .flatMap(book -> fileStorageService.saveBookCover(id, file, book.getCoverImage())
                    .flatMap(path -> {
                        book.setCoverImage(path); // yo'lni saqlaymiz (avval saqlanmas edi — bug)
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
