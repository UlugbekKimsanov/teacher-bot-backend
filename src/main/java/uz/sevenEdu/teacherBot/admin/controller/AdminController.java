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
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
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
import uz.sevenEdu.teacherBot.user.util.PhoneNumberUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final uz.sevenEdu.teacherBot.lesson.service.ExerciseAdminService exerciseAdminService;
    private final TestRepository testRepository;
    private final BooksRepository booksRepository;
    private final uz.sevenEdu.teacherBot.books.service.BooksService booksService;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserCourseRepository userCourseRepository;
    private final FileStorageService fileStorageService;
    private final uz.sevenEdu.teacherBot.common.service.MediaProbeService mediaProbeService;
    private final NewsRepository newsRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final uz.sevenEdu.teacherBot.payment.repository.PaymentMethodRepository paymentMethodRepository;
    private final uz.sevenEdu.teacherBot.payment.order.repository.PaymentOrderRepository paymentOrderRepository;
    private final uz.sevenEdu.teacherBot.rating.repository.AttendanceRepository attendanceRepository;
    private final uz.sevenEdu.teacherBot.notification.service.NotificationService notificationService;
    private final uz.sevenEdu.teacherBot.breakmusic.repository.BreakGroupRepository breakGroupRepository;
    private final uz.sevenEdu.teacherBot.breakmusic.repository.BreakTrackRepository breakTrackRepository;
    private final uz.sevenEdu.teacherBot.lesson.repository.AudiobookRepository audiobookRepository;
    private final uz.sevenEdu.teacherBot.notification.repository.AdminNotificationLogRepository adminNotificationLogRepository;
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
    public Mono<ApiResponse<List<BaseUser>>> getUsers(Authentication auth,
                                                      @RequestParam(required = false) String role,
                                                      @RequestParam(required = false) Long courseId) {
        return getAdminUser(auth).flatMap(admin -> {
            // Kurs bo'yicha filter — shu kursga yozilgan o'quvchilar (mehmonsiz)
            if (courseId != null) {
                return userCourseRepository.findByCourseId(courseId)
                        .map(uc -> uc.getUserId())
                        .collectList()
                        .flatMap(ids -> ids.isEmpty()
                                ? Mono.just(ApiResponse.ok(List.<BaseUser>of()))
                                : userRepository.findAllById(ids)
                                    .filter(u -> !Boolean.TRUE.equals(u.getIsGuest()))
                                    .collectList()
                                    .map(ApiResponse::ok));
            }
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
            UserRole newRole = parseRole(body.get("role"), UserRole.STUDENT);

            // isDefault=false admin faqat TEACHER va STUDENT yarata oladi
            boolean isDefault = Boolean.TRUE.equals(admin.getIsDefault());
            if (!isDefault && isAdminRole(newRole)) {
                return Mono.error(new ForbiddenException("Sizda admin yaratish huquqi yo'q"));
            }

            String firstName = trimToNull(body.get("firstName"));
            if (firstName == null) return Mono.error(new BadRequestException("Ism majburiy"));
            String email = normalizeEmail(body.get("email"));
            String phone = PhoneNumberUtil.normalizeOptionalUzbekPhone(body.get("phone"));
            if (email == null && phone == null) {
                return Mono.error(new BadRequestException("Email yoki telefon raqam majburiy"));
            }
            String password = body.get("password");
            if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
                return Mono.error(new BadRequestException(
                        "Parol kamida " + MIN_PASSWORD_LENGTH + " belgidan iborat bo'lishi kerak"));
            }

            BaseUser user = BaseUser.builder()
                    .firstName(firstName)
                    .lastName(trimToNull(body.get("lastName")))
                    .email(email)
                    .phone(phone)
                    .password(passwordEncoder.encode(password))
                    .role(newRole)
                    .specialization(trimToNull(body.get("specialization")))
                    .isDefault(false) // yangi admin har doim isDefault=false
                    .ball(0L)
                    .createdAt(LocalDateTime.now())
                    .build();
            return ensureContactsFree(email, phone, null)
                    .then(Mono.defer(() -> userRepository.save(user)))
                    .map(ApiResponse::ok);
        });
    }

    /**
     * Foydalanuvchini tahrirlash.
     * isDefault=true admin: barcha userlarni tahrirlashi mumkin (o'zidan tashqari).
     * isDefault=false admin: faqat TEACHER va STUDENT ni tahrirlashi mumkin.
     * "password" yuborilsa (kamida 6 belgi) — parol almashtiriladi.
     */
    @PutMapping("/users/{id}")
    public Mono<ApiResponse<BaseUser>> updateUser(Authentication auth, @PathVariable Long id, @RequestBody Map<String, String> body) {
        return getAdminUser(auth).flatMap(admin -> {
            boolean isDefault = Boolean.TRUE.equals(admin.getIsDefault());

            // O'zini tahrirlashga ruxsat yo'q
            if (admin.getId().equals(id)) {
                return Mono.error(new BadRequestException("O'zingizni tahrirlash mumkin emas"));
            }

            return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Foydalanuvchi topilmadi")))
                .flatMap(user -> {
                    if (isSystemGuest(user)) {
                        return Mono.error(new BadRequestException("Mehmon tizim akkauntini o'zgartirib bo'lmaydi"));
                    }
                    // isDefault=false admin faqat teacher/student tahrirlashi mumkin
                    if (!isDefault && isAdminRole(user.getRole())) {
                        return Mono.error(new ForbiddenException("Sizda admin tahrirlash huquqi yo'q"));
                    }

                    if (body.containsKey("firstName")) {
                        String firstName = trimToNull(body.get("firstName"));
                        if (firstName == null) return Mono.error(new BadRequestException("Ism majburiy"));
                        user.setFirstName(firstName);
                    }
                    if (body.containsKey("lastName")) user.setLastName(trimToNull(body.get("lastName")));
                    if (body.containsKey("email")) user.setEmail(normalizeEmail(body.get("email")));
                    if (body.containsKey("phone")) {
                        user.setPhone(PhoneNumberUtil.normalizeOptionalUzbekPhone(body.get("phone")));
                    }
                    if (body.containsKey("specialization")) user.setSpecialization(trimToNull(body.get("specialization")));

                    // Rolni o'zgartirish
                    if (body.containsKey("role")) {
                        UserRole newRole = parseRole(body.get("role"), null);
                        if (!isDefault && isAdminRole(newRole)) {
                            return Mono.error(new ForbiddenException("Sizda admin roli berish huquqi yo'q"));
                        }
                        if (Boolean.TRUE.equals(user.getIsDefault()) && newRole != UserRole.ADMIN) {
                            return Mono.error(new BadRequestException("Bosh adminning rolini o'zgartirib bo'lmaydi"));
                        }
                        user.setRole(newRole);
                    }

                    String password = body.get("password");
                    if (password != null && !password.isEmpty()) {
                        if (password.length() < MIN_PASSWORD_LENGTH) {
                            return Mono.error(new BadRequestException(
                                    "Parol kamida " + MIN_PASSWORD_LENGTH + " belgidan iborat bo'lishi kerak"));
                        }
                        user.setPassword(passwordEncoder.encode(password));
                    }

                    return ensureContactsFree(
                                    body.containsKey("email") ? user.getEmail() : null,
                                    body.containsKey("phone") ? user.getPhone() : null,
                                    id)
                            .then(Mono.defer(() -> userRepository.save(user)));
                })
                .map(ApiResponse::ok);
        });
    }

    /**
     * Foydalanuvchini o'chirish.
     * isDefault=true admin: barcha userlarni o'chirishi mumkin (o'zidan tashqari).
     * isDefault=false admin: faqat TEACHER va STUDENT ni o'chirishi mumkin.
     * Mehmon tizim akkaunti va bosh admin hech qachon o'chirilmaydi.
     */
    @DeleteMapping("/users/{id}")
    public Mono<ApiResponse<Void>> deleteUser(Authentication auth, @PathVariable Long id) {
        return getAdminUser(auth).flatMap(admin -> {
            boolean isDefault = Boolean.TRUE.equals(admin.getIsDefault());

            if (admin.getId().equals(id)) {
                return Mono.error(new BadRequestException("O'zingizni o'chirish mumkin emas"));
            }

            return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Foydalanuvchi topilmadi")))
                .flatMap(user -> {
                    if (isSystemGuest(user)) {
                        // Mobil ilovadagi "Mehmon sifatida kirish" shu akkauntga bog'liq
                        return Mono.error(new BadRequestException("Mehmon tizim akkauntini o'chirib bo'lmaydi"));
                    }
                    if (Boolean.TRUE.equals(user.getIsDefault())) {
                        return Mono.error(new BadRequestException("Bosh adminni o'chirib bo'lmaydi"));
                    }
                    if (!isDefault && isAdminRole(user.getRole())) {
                        return Mono.error(new ForbiddenException("Sizda admin o'chirish huquqi yo'q"));
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
                .switchIfEmpty(Mono.error(new NotFoundException("Til topilmadi")))
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
                // concatMap — guruhlar tartibi (order_index) saqlanadi
                .concatMap(group -> breakTrackRepository.findByGroupIdOrderById(group.getId())
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

    /** Yangi guruh yaratish: name (majburiy), icon (ixtiyoriy emoji). Ro'yxat oxiriga qo'shiladi. */
    @PostMapping("/break-groups")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.breakmusic.entity.BreakGroup>> createBreakGroup(
            Authentication auth, @RequestBody Map<String, Object> body) {
        return requireAdmin(auth).then(Mono.defer(() -> {
            String name = breakGroupName(body.get("name"));
            String icon = breakGroupIcon(body.get("icon"));
            return breakGroupRepository.findAllByOrderByOrderIndexAsc().collectList()
                .flatMap(all -> {
                    int nextOrder = all.stream()
                            .mapToInt(g -> g.getOrderIndex() != null ? g.getOrderIndex() : 0)
                            .max().orElse(0) + 1;
                    return breakGroupRepository.save(uz.sevenEdu.teacherBot.breakmusic.entity.BreakGroup.builder()
                            .name(name).icon(icon).orderIndex(nextOrder).build());
                })
                .map(g -> { g.setTracks(List.of()); return ApiResponse.ok(g); });
        }));
    }

    /** Guruhni tahrirlash — faqat yuborilgan maydonlar (name, icon, orderIndex) o'zgaradi. */
    @PutMapping("/break-groups/{id}")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.breakmusic.entity.BreakGroup>> updateBreakGroup(
            Authentication auth, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        return requireAdmin(auth).then(
            breakGroupRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Guruh topilmadi")))
                .flatMap(g -> {
                    if (body.containsKey("name")) g.setName(breakGroupName(body.get("name")));
                    if (body.containsKey("icon")) g.setIcon(breakGroupIcon(body.get("icon")));
                    if (body.get("orderIndex") != null) {
                        try {
                            g.setOrderIndex(Integer.valueOf(body.get("orderIndex").toString()));
                        } catch (NumberFormatException e) {
                            return Mono.error(new BadRequestException("orderIndex butun son bo'lishi kerak"));
                        }
                    }
                    return breakGroupRepository.save(g);
                })
                .map(g -> {
                    g.setBackgroundImage(fileStorageService.toPublicUrl(g.getBackgroundImage()));
                    return ApiResponse.ok(g);
                })
        );
    }

    /** Guruhni o'chirish — treklari (DB cascade) va barcha fayllari bilan birga. */
    @DeleteMapping("/break-groups/{id}")
    public Mono<ApiResponse<Void>> deleteBreakGroup(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            breakGroupRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Guruh topilmadi")))
                .flatMap(g -> breakTrackRepository.findByGroupIdOrderById(id).collectList()
                    .flatMap(tracks -> {
                        List<String> files = new ArrayList<>();
                        if (g.getBackgroundImage() != null) files.add(g.getBackgroundImage());
                        tracks.forEach(t -> { if (t.getFilePath() != null) files.add(t.getFilePath()); });
                        return breakGroupRepository.deleteById(id)
                                .then(fileStorageService.deleteStoredFiles(files));
                    }))
                .then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    /** Guruh fon rasmini yuklash. */
    @PostMapping(value = "/break-groups/{id}/upload-bg", consumes = "multipart/form-data")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.breakmusic.entity.BreakGroup>> uploadBreakBg(
            Authentication auth, @PathVariable Long id, @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            breakGroupRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Guruh topilmadi")))
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
                .switchIfEmpty(Mono.error(new NotFoundException("Guruh topilmadi")))
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
            breakTrackRepository.findById(id)
                .flatMap(t -> breakTrackRepository.deleteById(id)
                        .then(fileStorageService.deleteStoredFiles(pathsOf(t.getFilePath()))))
                .then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
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
        return getAdminUser(auth).flatMap(admin -> {
            String target = String.valueOf(body.getOrDefault("target", "ALL")).toUpperCase();
            String title = body.get("title") != null ? body.get("title").toString().trim() : "";
            String text = body.get("body") != null ? body.get("body").toString().trim() : "";
            String image = body.get("image") != null && !body.get("image").toString().isBlank()
                    ? body.get("image").toString().trim() : null; // optional
            if (title.isEmpty() || text.isEmpty()) {
                return Mono.error(new BadRequestException("Sarlavha va matn majburiy"));
            }
            if (title.length() > MAX_NOTIFICATION_TITLE) {
                return Mono.error(new BadRequestException("Sarlavha " + MAX_NOTIFICATION_TITLE + " belgidan oshmasligi kerak"));
            }
            if (text.length() > MAX_NOTIFICATION_BODY) {
                return Mono.error(new BadRequestException("Matn " + MAX_NOTIFICATION_BODY + " belgidan oshmasligi kerak"));
            }
            if (image != null && image.length() > 500) {
                return Mono.error(new BadRequestException("Rasm havolasi juda uzun"));
            }

            Long logCourseId = null;
            reactor.core.publisher.Flux<Long> userIds;
            switch (target) {
                case "STUDENTS" -> userIds = userRepository.findByRole("STUDENT").map(BaseUser::getId);
                case "TEACHERS" -> userIds = userRepository.findByRole("TEACHER").map(BaseUser::getId);
                case "COURSE" -> {
                    Long courseId = toLongOrNull(body.get("courseId"));
                    if (courseId == null) return Mono.error(new BadRequestException("courseId majburiy"));
                    logCourseId = courseId;
                    userIds = userCourseRepository.findByCourseId(courseId)
                            .map(uz.sevenEdu.teacherBot.course.entity.UserCourse::getUserId);
                }
                case "USERS" -> {
                    Object raw = body.get("userIds");
                    if (!(raw instanceof List<?> list) || list.isEmpty()) {
                        return Mono.error(new BadRequestException("userIds majburiy"));
                    }
                    java.util.List<Long> ids = new ArrayList<>();
                    for (Object o : list) {
                        Long uid = toLongOrNull(o);
                        if (uid == null) return Mono.error(new BadRequestException("userIds noto'g'ri"));
                        ids.add(uid);
                    }
                    userIds = reactor.core.publisher.Flux.fromIterable(ids);
                }
                default -> {
                    if (!"ALL".equals(target)) {
                        return Mono.error(new BadRequestException("target noto'g'ri"));
                    }
                    userIds = userRepository.findStudentsAndTeachers().map(BaseUser::getId);
                }
            }

            final Long courseIdForLog = logCourseId;
            return userIds.collectList()
                    .flatMap(ids -> notificationService.sendBulk(ids, title, text, "ADMIN", image)
                            .flatMap(sent -> adminNotificationLogRepository.save(
                                            uz.sevenEdu.teacherBot.notification.entity.AdminNotificationLog.builder()
                                                    .title(title).body(text).image(image)
                                                    .target(target).courseId(courseIdForLog)
                                                    .recipientCount(sent.intValue())
                                                    .sentBy(admin.getId())
                                                    .createdAt(LocalDateTime.now())
                                                    .build())
                                    // Tarixga yozish — ikkilamchi amal: xato bo'lsa ham xabar yuborilgan hisoblanadi
                                    .onErrorResume(e -> Mono.empty())
                                    .thenReturn(sent))
                            .map(sent -> {
                                Map<String, Object> res = new HashMap<>();
                                res.put("sent", sent);
                                return ApiResponse.ok("Yuborildi", res);
                            }));
        });
    }

    /** Yuborilgan bildirishnomalar tarixi — yangidan eskiga (limit: 1..200, standart 50). */
    @GetMapping("/notifications/history")
    public Mono<ApiResponse<List<uz.sevenEdu.teacherBot.notification.entity.AdminNotificationLog>>> notificationHistory(
            Authentication auth, @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return requireAdmin(auth).then(
            adminNotificationLogRepository.findRecent(safeLimit).collectList().map(ApiResponse::ok)
        );
    }

    /** Bildirishnoma uchun rasm yuklash (optional). Public URL qaytaradi — keyin /notifications'ga image sifatida yuboriladi. */
    @PostMapping(value = "/notifications/upload-image", consumes = "multipart/form-data")
    public Mono<ApiResponse<Map<String, Object>>> uploadNotificationImage(Authentication auth,
                                                                          @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            fileStorageService.saveNotificationImage(file)
                .map(path -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("path", path);
                    res.put("url", fileStorageService.toPublicUrl(path));
                    return ApiResponse.ok("Yuklandi", res);
                })
        );
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
                .switchIfEmpty(Mono.error(new NotFoundException("To'lov usuli topilmadi")))
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
        return requireAdmin(auth).then(Mono.defer(() -> {
            validateCourse(course);
            course.setId(null); // yaratishda id clientdan qabul qilinmaydi
            course.setName(course.getName().trim());
            return courseRepository.save(course).map(ApiResponse::ok);
        }));
    }

    @PutMapping("/courses/{id}")
    public Mono<ApiResponse<Course>> updateCourse(Authentication auth, @PathVariable Long id, @RequestBody Course body) {
        return requireAdmin(auth).then(
            courseRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(c -> {
                    validateCourse(body);
                    c.setName(body.getName().trim());
                    c.setLanguageId(body.getLanguageId());
                    c.setFlagEmoji(body.getFlagEmoji());
                    c.setGoal(body.getGoal());
                    c.setAuthor(body.getAuthor());
                    c.setIsPremium(body.getIsPremium());
                    // coverImage/imageId form'da yuborilmaydi (alohida upload qilinadi) —
                    // null bo'lsa mavjud rasmni O'CHIRMAYMIZ (aks holda har saqlashda cover yo'qoladi).
                    if (body.getCoverImage() != null) c.setCoverImage(body.getCoverImage());
                    if (body.getImageId() != null) c.setImageId(body.getImageId());
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
            courseRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                // Darslar va audio kitoblar DB'da cascade bilan o'chadi — fayllarini oldindan yig'ib olamiz
                .flatMap(course -> lessonRepository.findByCourseIdOrderByOrderIndex(id)
                    .concatMap(this::lessonFiles)
                    .collectList()
                    .flatMap(lessonFileLists -> {
                        List<String> files = new ArrayList<>();
                        lessonFileLists.forEach(files::addAll);
                        files.addAll(pathsOf(course.getCoverImage(), course.getBackgroundImage()));
                        return courseRepository.deleteById(id)
                                .then(deleteUnreferencedLessonFiles(files));
                    }))
                .then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    /** Kurs card (cover) rasmini olib tashlash. */
    @DeleteMapping("/courses/{id}/cover")
    public Mono<ApiResponse<Course>> deleteCourseCover(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            courseRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course -> {
                    String old = course.getCoverImage();
                    course.setCoverImage(null);
                    return courseRepository.save(course)
                            .flatMap(saved -> fileStorageService.deleteStoredFiles(pathsOf(old)).thenReturn(saved));
                })
                .map(ApiResponse::ok)
        );
    }

    /** Kurs background rasmini olib tashlash. */
    @DeleteMapping("/courses/{id}/background")
    public Mono<ApiResponse<Course>> deleteCourseBackground(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            courseRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course -> {
                    String old = course.getBackgroundImage();
                    course.setBackgroundImage(null);
                    return courseRepository.save(course)
                            .flatMap(saved -> fileStorageService.deleteStoredFiles(pathsOf(old)).thenReturn(saved));
                })
                .map(ApiResponse::ok)
        );
    }

    /** Kurs card (cover) rasmini yuklash. */
    @PostMapping(value = "/courses/{id}/upload-cover", consumes = "multipart/form-data")
    public Mono<ApiResponse<Course>> uploadCourseCover(Authentication auth, @PathVariable Long id,
                                                       @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            courseRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
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
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
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
                .switchIfEmpty(Mono.error(new NotFoundException("Til topilmadi")))
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
            userRepository.findById(teacherId)
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .switchIfEmpty(Mono.error(new NotFoundException("O'qituvchi topilmadi")))
                .then(courseRepository.existsById(courseId))
                .flatMap(exists -> exists
                        ? courseTeacherRepository.findByCourseIdAndTeacherId(courseId, teacherId)
                            .switchIfEmpty(Mono.defer(() -> courseTeacherRepository.save(
                                    CourseTeacher.builder().courseId(courseId).teacherId(teacherId).build())))
                        : Mono.error(new NotFoundException("Kurs topilmadi")))
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
            if (lesson.getCourseId() == null) return Mono.error(new BadRequestException("Kurs tanlanmagan"));
            if (lesson.getName() == null || lesson.getName().isBlank()) {
                return Mono.error(new BadRequestException("Dars nomi majburiy"));
            }
            Lesson l = Lesson.builder()
                    .courseId(lesson.getCourseId())
                    .name(lesson.getName())
                    .description(lesson.getDescription())
                    .orderIndex(lesson.getOrderIndex())
                    .durationSec(lesson.getDurationSec())
                    .coverImage(lesson.getCoverImage())
                    .videoUrl(lesson.getVideoUrl())
                    .youtubeId(lesson.getYoutubeId())
                    .build();
            return courseRepository.existsById(lesson.getCourseId())
                    .flatMap(exists -> exists
                            ? lessonRepository.save(l)
                            : Mono.<Lesson>error(new NotFoundException("Kurs topilmadi")))
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
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
                .flatMap(l -> {
                    if (body.getName() != null && body.getName().isBlank()) {
                        return Mono.error(new BadRequestException("Dars nomi majburiy"));
                    }
                    // Faqat berilgan maydonlar yangilanadi — video/rasm yuborilmasa,
                    // eskisi o'zgarishsiz qoladi (cover/video alohida upload endpointlar orqali boshqariladi).
                    if (body.getName() != null) l.setName(body.getName());
                    if (body.getDescription() != null) l.setDescription(body.getDescription());
                    if (body.getOrderIndex() != null) l.setOrderIndex(body.getOrderIndex());
                    if (body.getDurationSec() != null) l.setDurationSec(body.getDurationSec());
                    if (body.getCoverImage() != null && !body.getCoverImage().isBlank()) l.setCoverImage(body.getCoverImage());
                    if (body.getVideoUrl() != null && !body.getVideoUrl().isBlank()) l.setVideoUrl(body.getVideoUrl());
                    // YouTube ID: null — o'zgarmaydi, bo'sh satr — biriktiruv olib tashlanadi.
                    if (body.getYoutubeId() != null) {
                        l.setYoutubeId(body.getYoutubeId().isBlank() ? null : body.getYoutubeId().trim());
                    }
                    return lessonRepository.save(l);
                })
                .map(ApiResponse::ok)
        );
    }

    /**
     * Dars videosini yuklash (URL emas, fayl orqali). Maks. hajm 2 GB.
     *
     * <p>Davomiylik avtomatik aniqlanadi: fayl saqlangandan keyin ffprobe bilan
     * o'lchanadi va {@code durationSec} shu qiymat bilan yangilanadi. ffprobe
     * mavjud bo'lmasa — brauzerda aniqlangan qiymat o'zgarishsiz qoladi,
     * ya'ni yuklash hech qachon shu sabab uzilmaydi.
     */
    @PostMapping(value = "/lessons/{id}/upload-video", consumes = "multipart/form-data")
    public Mono<ApiResponse<Lesson>> uploadLessonVideo(Authentication auth, @PathVariable Long id,
                                                       @RequestPart("file") FilePart file) {
        return requireAdmin(auth).then(
            lessonRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
                .flatMap(lesson -> fileStorageService.saveLessonVideo(id, file, lesson.getVideoUrl())
                    .flatMap(path -> {
                        lesson.setVideoUrl(path);
                        return mediaProbeService
                                .probeDurationSec(fileStorageService.resolveStoredPath(path))
                                .doOnNext(lesson::setDurationSec)
                                .then(Mono.defer(() -> lessonRepository.save(lesson)));
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
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
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
            lessonRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
                .flatMap(lesson -> lessonFiles(lesson)
                    .flatMap(files -> lessonRepository.deleteById(id)
                            .then(deleteUnreferencedLessonFiles(files))))
                .then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    /** Dars muqova rasmini olib tashlash. */
    @DeleteMapping("/lessons/{id}/cover")
    public Mono<ApiResponse<Lesson>> deleteLessonCover(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            lessonRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
                .flatMap(lesson -> {
                    String old = lesson.getCoverImage();
                    lesson.setCoverImage(null);
                    return lessonRepository.save(lesson)
                            .flatMap(saved -> fileStorageService.deleteStoredFiles(pathsOf(old)).thenReturn(saved));
                })
                .map(ApiResponse::ok)
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

    @PutMapping("/vocabulary/{id}")
    public Mono<ApiResponse<Vocabulary>> updateVocab(Authentication auth, @PathVariable Long id, @RequestBody Vocabulary body) {
        return requireAdmin(auth).then(
            vocabularyRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Lug'at topilmadi")))
                .flatMap(v -> {
                    if (body.getTranslationUz() != null) v.setTranslationUz(body.getTranslationUz());
                    if (body.getTranslationTarget() != null) v.setTranslationTarget(body.getTranslationTarget());
                    return vocabularyRepository.save(v);
                })
                .map(ApiResponse::ok)
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
                .switchIfEmpty(Mono.error(new NotFoundException("Dars testi topilmadi")))
                .flatMap(test -> {
                    q.setTestId(test.getId());
                    return questionRepository.save(q);
                })
                .map(ApiResponse::ok)
        );
    }

    @PutMapping("/questions/{id}")
    public Mono<ApiResponse<Question>> updateQuestion(Authentication auth, @PathVariable Long id, @RequestBody Question body) {
        return requireAdmin(auth).then(
            questionRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Savol topilmadi")))
                .flatMap(q -> {
                    if (body.getQuestionText() != null) q.setQuestionText(body.getQuestionText());
                    if (body.getOptionA() != null) q.setOptionA(body.getOptionA());
                    if (body.getOptionB() != null) q.setOptionB(body.getOptionB());
                    if (body.getOptionC() != null) q.setOptionC(body.getOptionC());
                    if (body.getOptionD() != null) q.setOptionD(body.getOptionD());
                    if (body.getCorrectOption() != null) q.setCorrectOption(body.getCorrectOption());
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

    // Ko'p bo'shliqli mashqlar: gap ('_' = bo'shliq) + chalg'ituvchilar + har bo'shliqning to'g'ri javobi.
    // Validatsiya va bo'shliqlarni saqlash ExerciseAdminService da.

    @GetMapping("/lessons/{lessonId}/exercises")
    public Mono<ApiResponse<List<uz.sevenEdu.teacherBot.lesson.dto.ExerciseAdminDto>>> getExercises(
            Authentication auth, @PathVariable Long lessonId) {
        return requireAdmin(auth).then(
            exerciseAdminService.list(lessonId).collectList().map(ApiResponse::ok)
        );
    }

    @PostMapping("/lessons/{lessonId}/exercises")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.lesson.dto.ExerciseAdminDto>> createExercise(
            Authentication auth, @PathVariable Long lessonId,
            @RequestBody uz.sevenEdu.teacherBot.lesson.dto.ExerciseUpsertRequest req) {
        return requireAdmin(auth).then(
            exerciseAdminService.create(lessonId, req).map(ApiResponse::ok)
        );
    }

    @PutMapping("/exercises/{id}")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.lesson.dto.ExerciseAdminDto>> updateExercise(
            Authentication auth, @PathVariable Long id,
            @RequestBody uz.sevenEdu.teacherBot.lesson.dto.ExerciseUpsertRequest req) {
        return requireAdmin(auth).then(
            exerciseAdminService.update(id, req).map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/exercises/{id}")
    public Mono<ApiResponse<Void>> deleteExercise(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            exerciseAdminService.delete(id).then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
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

    /** Darsga audio kitob qo'shish: audio (majburiy) + PDF (ixtiyoriy) + nom + tavsif. */
    @PostMapping(value = "/lessons/{lessonId}/audiobooks", consumes = "multipart/form-data")
    public Mono<ApiResponse<uz.sevenEdu.teacherBot.lesson.entity.Audiobook>> uploadAudiobook(
            Authentication auth, @PathVariable Long lessonId,
            @RequestPart("file") FilePart file,
            @RequestPart(value = "pdf", required = false) FilePart pdf,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        return requireAdmin(auth).then(
            lessonRepository.findById(lessonId)
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
                .then(audiobookRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList())
                .flatMap(existing -> fileStorageService.saveLessonAudiobook(lessonId, file)
                    .flatMap(audioPath -> {
                        Mono<String> pdfMono = pdf != null
                                ? fileStorageService.saveLessonAudiobookPdf(lessonId, pdf)
                                    // PDF rad etilsa, endigina saqlangan audio diskda yetim qolmasin
                                    .onErrorResume(e -> fileStorageService.deleteStoredFiles(pathsOf(audioPath))
                                            .then(Mono.error(e)))
                                : Mono.just("");
                        return pdfMono.flatMap(pdfPath -> audiobookRepository.save(
                                uz.sevenEdu.teacherBot.lesson.entity.Audiobook.builder()
                                        .lessonId(lessonId)
                                        .title(title != null && !title.isBlank() ? title : file.filename())
                                        .description(description)
                                        .filePath(audioPath)
                                        .pdfPath(pdfPath.isBlank() ? null : pdfPath)
                                        .orderIndex(existing.size() + 1)
                                        .createdAt(java.time.LocalDateTime.now())
                                        .build()));
                    }))
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/audiobooks/{id}")
    public Mono<ApiResponse<Void>> deleteAudiobook(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            audiobookRepository.findById(id)
                .flatMap(a -> audiobookRepository.deleteById(id)
                        .then(fileStorageService.deleteStoredFiles(pathsOf(a.getFilePath(), a.getPdfPath()))))
                .then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
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
        return requireAdmin(auth).then(Mono.defer(() -> {
            if (book.getTitle() == null || book.getTitle().isBlank()) {
                return Mono.error(new BadRequestException("Kitob nomi majburiy"));
            }
            book.setId(null); // yaratishda id clientdan qabul qilinmaydi
            // Fayl/muqova/galereya faqat upload endpointlari orqali o'rnatiladi
            book.setFilePath(null);
            book.setCoverImage(null);
            book.setImages(null);
            if (book.getCreatedAt() == null) book.setCreatedAt(LocalDateTime.now());
            return booksRepository.save(book).map(ApiResponse::ok);
        }));
    }

    @PutMapping("/books/{id}")
    public Mono<ApiResponse<Books>> updateBook(Authentication auth, @PathVariable Long id, @RequestBody Books body) {
        return requireAdmin(auth).then(
            booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi")))
                .flatMap(b -> {
                    if (body.getTitle() == null || body.getTitle().isBlank()) {
                        return Mono.error(new BadRequestException("Kitob nomi majburiy"));
                    }
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
                    b.setDeliveryType(body.getDeliveryType());
                    b.setDeliveryPrice(body.getDeliveryPrice());
                    // format/daraja olib tashlandi; reyting qo'lda emas (baholar asosida)
                    b.setLanguage(body.getLanguage());
                    b.setPreviewPages(body.getPreviewPages());
                    // Fayl maydonlari forma orqali O'CHIRILMAYDI: filePath/coverImage/images faqat
                    // upload va "olib tashlash" endpointlari orqali o'zgaradi (aks holda har
                    // "Saqlash" yuklangan PDF yo'lini null qilib yuborardi).
                    if (body.getImageId() != null) b.setImageId(body.getImageId());
                    if (body.getFileId() != null) b.setFileId(body.getFileId());
                    b.setUpdatedAt(LocalDateTime.now());
                    return booksRepository.save(b);
                })
                .map(ApiResponse::ok)
        );
    }

    @DeleteMapping("/books/{id}")
    public Mono<ApiResponse<Void>> deleteBook(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi")))
                .flatMap(book -> {
                    List<String> files = pathsOf(book.getFilePath(), book.getCoverImage());
                    files.addAll(booksService.galleryPaths(book));
                    return booksRepository.deleteById(id)
                            .then(fileStorageService.deleteStoredFiles(files));
                })
                .then(Mono.just(ApiResponse.ok("O'chirildi", (Void) null)))
        );
    }

    /** Kitob faylini (PDF/EPUB) olib tashlash. */
    @DeleteMapping("/books/{id}/file")
    public Mono<ApiResponse<Books>> deleteBookFile(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi")))
                .flatMap(book -> {
                    String old = book.getFilePath();
                    book.setFilePath(null);
                    book.setUpdatedAt(LocalDateTime.now());
                    return booksRepository.save(book)
                            .flatMap(saved -> fileStorageService.deleteStoredFiles(pathsOf(old)).thenReturn(saved));
                })
                .map(ApiResponse::ok)
        );
    }

    /** Kitob muqova rasmini olib tashlash. */
    @DeleteMapping("/books/{id}/cover")
    public Mono<ApiResponse<Books>> deleteBookCover(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth).then(
            booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi")))
                .flatMap(book -> {
                    String old = book.getCoverImage();
                    book.setCoverImage(null);
                    book.setUpdatedAt(LocalDateTime.now());
                    return booksRepository.save(book)
                            .flatMap(saved -> fileStorageService.deleteStoredFiles(pathsOf(old)).thenReturn(saved));
                })
                .map(ApiResponse::ok)
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
                .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi")))
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
                .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi")))
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

    /**
     * Kitob galereyasiga rasm qo'shish (bir nechta rasm saqlanadi)
     */
    @PostMapping(value = "/books/{id}/upload-image", consumes = "multipart/form-data")
    public Mono<ApiResponse<Books>> uploadBookImage(Authentication auth, @PathVariable Long id,
                                                     @RequestPart("file") FilePart file) {
        return requireAdmin(auth)
                .then(booksService.addBookImage(id, file))
                .map(ApiResponse::ok);
    }

    /**
     * Kitob galereyasidan rasmni o'chirish (path — bazadagi nisbiy yo'l)
     */
    @DeleteMapping("/books/{id}/images")
    public Mono<ApiResponse<Books>> deleteBookImage(Authentication auth, @PathVariable Long id,
                                                     @RequestParam("path") String path) {
        return requireAdmin(auth)
                .then(booksService.removeBookImage(id, path))
                .map(ApiResponse::ok);
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
                .switchIfEmpty(Mono.error(new NotFoundException("Yangilik topilmadi")))
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

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_NOTIFICATION_TITLE = 150;
    private static final int MAX_NOTIFICATION_BODY = 1000;
    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Long toLongOrNull(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Null/bo'sh bo'lmagan yo'llardan o'zgaruvchan ro'yxat. */
    private static List<String> pathsOf(String... paths) {
        List<String> result = new ArrayList<>();
        for (String path : paths) {
            if (path != null && !path.isBlank()) result.add(path);
        }
        return result;
    }

    private static boolean isAdminRole(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.HEAD_ADMIN;
    }

    /** Mobil "Mehmon sifatida kirish" bog'langan tizim akkaunti (seed id 999). */
    private static boolean isSystemGuest(BaseUser user) {
        return Boolean.TRUE.equals(user.getIsGuest())
                || uz.sevenEdu.teacherBot.common.util.GuestUtil.GUEST_USER_ID.equals(user.getId());
    }

    private static UserRole parseRole(String raw, UserRole fallback) {
        if (raw == null || raw.isBlank()) {
            if (fallback != null) return fallback;
            throw new BadRequestException("Rol majburiy");
        }
        try {
            return UserRole.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Rol noto'g'ri: " + raw);
        }
    }

    private static String normalizeEmail(String raw) {
        String email = trimToNull(raw);
        if (email == null) return null;
        if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Email noto'g'ri formatda");
        }
        return email;
    }

    /** Email/telefon boshqa foydalanuvchida band bo'lsa — 400 (aks holda UNIQUE cheklovi 500 qaytarardi). */
    private Mono<Void> ensureContactsFree(String email, String phone, Long selfId) {
        Mono<Void> emailCheck = email == null ? Mono.empty()
                : userRepository.findByEmailIgnoreCase(email)
                    .filter(u -> !u.getId().equals(selfId))
                    .flatMap(u -> Mono.<Void>error(new BadRequestException("Bu email allaqachon ro'yxatdan o'tgan")));
        Mono<Void> phoneCheck = phone == null ? Mono.empty()
                : userRepository.findByPhoneDigits(PhoneNumberUtil.digits(phone))
                    .filter(u -> !u.getId().equals(selfId))
                    .flatMap(u -> Mono.<Void>error(new BadRequestException("Bu telefon raqam allaqachon ro'yxatdan o'tgan")));
        return emailCheck.then(phoneCheck);
    }

    private static String breakGroupName(Object raw) {
        String name = raw != null ? raw.toString().trim() : "";
        if (name.isEmpty()) throw new BadRequestException("Guruh nomi majburiy");
        if (name.length() > 60) throw new BadRequestException("Guruh nomi 60 belgidan oshmasligi kerak");
        return name;
    }

    private static String breakGroupIcon(Object raw) {
        String icon = raw != null ? raw.toString().trim() : "";
        if (icon.isEmpty()) return null;
        if (icon.length() > 8) throw new BadRequestException("Ikonka 8 belgidan oshmasligi kerak");
        return icon;
    }

    /** Kurs yaratish/tahrirlash tekshiruvi: nom majburiy; premium kurs narxsiz bo'lmaydi. */
    private static void validateCourse(Course course) {
        if (course.getName() == null || course.getName().isBlank()) {
            throw new BadRequestException("Kurs nomi majburiy");
        }
        if (course.getPrice() != null && course.getPrice() < 0) {
            throw new BadRequestException("Narx manfiy bo'lishi mumkin emas");
        }
        if (Boolean.TRUE.equals(course.getIsPremium()) && (course.getPrice() == null || course.getPrice() <= 0)) {
            throw new BadRequestException("Premium kurs uchun narx majburiy");
        }
    }

    /** Darsga tegishli barcha fayllar: video (tashqi URL bo'lmasa), muqova, audio kitoblar va ularning PDF'lari. */
    /**
     * Darslar o'chirilgandan KEYIN chaqiriladi: boshqa dars hali ishlatayotgan faylga tegmaydi
     * (bazada nusxalangan darslar bitta video faylni bo'lishishi mumkin).
     */
    private Mono<Void> deleteUnreferencedLessonFiles(List<String> files) {
        return reactor.core.publisher.Flux.fromIterable(files)
                .filterWhen(path -> lessonRepository.countFileReferences(path).map(count -> count == 0))
                .collectList()
                .flatMap(fileStorageService::deleteStoredFiles)
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<List<String>> lessonFiles(Lesson lesson) {
        return audiobookRepository.findByLessonIdOrderByOrderIndex(lesson.getId()).collectList()
                .map(audiobooks -> {
                    List<String> files = pathsOf(lesson.getVideoUrl(), lesson.getCoverImage());
                    audiobooks.forEach(a -> files.addAll(pathsOf(a.getFilePath(), a.getPdfPath())));
                    return files;
                });
    }

    private Mono<Void> requireAdmin(Authentication auth) {
        if (auth == null) return Mono.error(new uz.sevenEdu.teacherBot.common.exception.UnauthorizedException("Avtorizatsiya talab qilinadi"));
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new uz.sevenEdu.teacherBot.common.exception.UnauthorizedException("Avtorizatsiya talab qilinadi")))
                .flatMap(user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        return Mono.error(new ForbiddenException("Faqat admin uchun"));
                    }
                    return Mono.empty();
                });
    }

    /** Admin userni qaytaradi (requireAdmin + user ma'lumotlari) */
    private Mono<BaseUser> getAdminUser(Authentication auth) {
        if (auth == null) return Mono.error(new uz.sevenEdu.teacherBot.common.exception.UnauthorizedException("Avtorizatsiya talab qilinadi"));
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new uz.sevenEdu.teacherBot.common.exception.UnauthorizedException("Avtorizatsiya talab qilinadi")))
                .flatMap(user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        return Mono.error(new ForbiddenException("Faqat admin uchun"));
                    }
                    return Mono.just(user);
                });
    }
}
