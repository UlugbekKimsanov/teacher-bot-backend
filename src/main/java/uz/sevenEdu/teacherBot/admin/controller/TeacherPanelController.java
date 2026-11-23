package uz.sevenEdu.teacherBot.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.chat.dto.ChatMessageDto;
import uz.sevenEdu.teacherBot.chat.entity.ChatMessage;
import uz.sevenEdu.teacherBot.chat.repository.ChatMessageRepository;
import uz.sevenEdu.teacherBot.chat.service.ChatService;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.common.security.RoleGuard;
import uz.sevenEdu.teacherBot.common.util.GuestUtil;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.entity.UserCourse;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;
import uz.sevenEdu.teacherBot.lesson.entity.TeacherQuestion;
import uz.sevenEdu.teacherBot.lesson.entity.UserLesson;
import uz.sevenEdu.teacherBot.lesson.repository.LessonRepository;
import uz.sevenEdu.teacherBot.lesson.repository.TeacherQuestionRepository;
import uz.sevenEdu.teacherBot.lesson.repository.UserLessonRepository;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ustoz paneli. "Ustozning kurslari" = biriktirilgan kurslar + mutaxassisligi (tillari)
 * mos kurslar — {@link ChatService#teacherCourseIds(Long)}; chatga kirish huquqi bilan bir xil qoida.
 */
@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherPanelController {

    /** Dashboard'da ko'rsatiladigan javob kutayotgan chatlar soni. */
    private static final int DASHBOARD_RECENT_LIMIT = 5;
    private static final int PREVIEW_LENGTH = 120;

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final LessonRepository lessonRepository;
    private final UserLessonRepository userLessonRepository;
    private final TeacherQuestionRepository teacherQuestionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatService chatService;
    private final RoleGuard roleGuard;

    // ── Dashboard ──────────────────────────────────────────────

    @GetMapping("/dashboard")
    public Mono<ApiResponse<Map<String, Object>>> dashboard(Authentication auth) {
        return requireTeacher(auth).flatMap(chatService::teacherCourseIds).flatMap(courseIds -> {
            if (courseIds.isEmpty()) {
                return Mono.just(ApiResponse.ok(dashboardStats(0, 0, List.of())));
            }
            return Mono.zip(
                    userCourseRepository.findByCourseIdIn(courseIds)
                            .map(UserCourse::getUserId)
                            .filter(id -> !GuestUtil.isGuest(id))
                            .distinct()
                            .count(),
                    buildRooms(courseIds)
            ).map(t -> ApiResponse.ok(dashboardStats(courseIds.size(), t.getT1(), t.getT2())));
        });
    }

    private Map<String, Object> dashboardStats(int courses, long students, List<Map<String, Object>> rooms) {
        List<Map<String, Object>> awaiting = rooms.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("awaitingReply")))
                .collect(Collectors.toList());
        long activeChats = rooms.stream().filter(r -> r.get("lastTime") != null).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("myCourses", courses);
        stats.put("myStudents", students);
        stats.put("awaitingChats", awaiting.size());
        stats.put("activeChats", activeChats);
        stats.put("recentAwaiting", awaiting.stream().limit(DASHBOARD_RECENT_LIMIT).collect(Collectors.toList()));
        // Eski mijozlar uchun moslik: savollar ham, o'qilmaganlar ham — javob kutayotgan chatlar
        stats.put("pendingQuestions", awaiting.size());
        stats.put("unreadChats", awaiting.size());
        return stats;
    }

    // ── My Courses ─────────────────────────────────────────────

    @GetMapping("/courses")
    public Mono<ApiResponse<List<Course>>> getMyCourses(Authentication auth) {
        return requireTeacher(auth).flatMap(chatService::teacherCourseIds).flatMap(courseIds -> {
            if (courseIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Course>emptyList()));
            return Mono.zip(
                    courseRepository.findAllById(courseIds).collectList(),
                    userCourseRepository.findByCourseIdIn(courseIds).collectList(),
                    lessonRepository.findByCourseIdInOrdered(courseIds).collectList()
            ).map(t -> {
                Map<Long, Long> students = t.getT2().stream()
                        .filter(uc -> !GuestUtil.isGuest(uc.getUserId()))
                        .collect(Collectors.groupingBy(UserCourse::getCourseId,
                                Collectors.mapping(UserCourse::getUserId,
                                        Collectors.collectingAndThen(Collectors.toSet(), s -> (long) s.size()))));
                Map<Long, Long> lessons = t.getT3().stream()
                        .collect(Collectors.groupingBy(Lesson::getCourseId, Collectors.counting()));
                List<Course> courses = new ArrayList<>(t.getT1());
                for (Course c : courses) {
                    c.setStudentCount(students.getOrDefault(c.getId(), 0L));
                    c.setLessonCount(lessons.getOrDefault(c.getId(), 0L));
                }
                courses.sort(Comparator
                        .comparing((Course c) -> c.getOrderIndex() != null ? c.getOrderIndex() : Integer.MAX_VALUE)
                        .thenComparing(c -> c.getName() != null ? c.getName() : ""));
                return ApiResponse.ok(courses);
            });
        });
    }

    // ── My Students ────────────────────────────────────────────

    @GetMapping("/students")
    public Mono<ApiResponse<List<Map<String, Object>>>> getMyStudents(Authentication auth) {
        return requireTeacher(auth).flatMap(chatService::teacherCourseIds).flatMap(courseIds -> {
            if (courseIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));

            return Mono.zip(
                    courseRepository.findAllById(courseIds).collectMap(Course::getId),
                    userCourseRepository.findByCourseIdIn(courseIds).collectList(),
                    lessonRepository.findByCourseIdInOrdered(courseIds).collectList()
            ).flatMap(t -> {
                Map<Long, Course> courses = t.getT1();
                List<UserCourse> userCourses = t.getT2().stream()
                        .filter(uc -> !GuestUtil.isGuest(uc.getUserId()))
                        .collect(Collectors.toList());
                // darsId -> kursId
                Map<Long, Long> lessonCourse = t.getT3().stream()
                        .collect(Collectors.toMap(Lesson::getId, Lesson::getCourseId, (a, b) -> a));
                Map<Long, Long> lessonsPerCourse = t.getT3().stream()
                        .collect(Collectors.groupingBy(Lesson::getCourseId, Collectors.counting()));
                Set<Long> studentIds = userCourses.stream().map(UserCourse::getUserId).collect(Collectors.toSet());
                if (studentIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));

                return Mono.zip(
                        userRepository.findAllById(studentIds).collectMap(BaseUser::getId),
                        userLessonRepository.findByUserIdIn(studentIds).collectList()
                ).map(u -> {
                    Map<Long, BaseUser> students = u.getT1();
                    // "userId:courseId" -> tugatilgan darslar soni
                    Map<String, Long> completed = u.getT2().stream()
                            .filter(ul -> Boolean.TRUE.equals(ul.getIsCompleted()) && lessonCourse.containsKey(ul.getLessonId()))
                            .collect(Collectors.groupingBy(
                                    (UserLesson ul) -> ul.getUserId() + ":" + lessonCourse.get(ul.getLessonId()),
                                    Collectors.mapping(UserLesson::getLessonId,
                                            Collectors.collectingAndThen(Collectors.toSet(), s -> (long) s.size()))));

                    List<Map<String, Object>> result = new ArrayList<>();
                    for (UserCourse uc : userCourses) {
                        BaseUser student = students.get(uc.getUserId());
                        Course course = courses.get(uc.getCourseId());
                        if (student == null || course == null) continue;

                        long totalLessons = lessonsPerCourse.getOrDefault(uc.getCourseId(), 0L);
                        long completedLessons = Math.min(totalLessons,
                                completed.getOrDefault(uc.getUserId() + ":" + uc.getCourseId(), 0L));

                        Map<String, Object> row = new HashMap<>();
                        row.put("id", uc.getId());
                        row.put("userId", student.getId());
                        row.put("firstName", student.getFirstName());
                        row.put("lastName", student.getLastName());
                        row.put("courseId", course.getId());
                        row.put("courseName", course.getName());
                        row.put("totalLessons", totalLessons);
                        row.put("lessonsCompleted", completedLessons);
                        row.put("progress", totalLessons > 0 ? Math.round(completedLessons * 100.0 / totalLessons) : 0);
                        row.put("enrolledAt", uc.getCreatedAt());
                        row.put("lastActiveAt", student.getLastActiveAt());
                        result.add(row);
                    }
                    result.sort(Comparator
                            .comparing((Map<String, Object> r) -> fullName(students.get((Long) r.get("userId"))).toLowerCase())
                            .thenComparing(r -> String.valueOf(r.get("courseName"))));
                    return ApiResponse.ok(result);
                });
            });
        });
    }

    // ── Teacher Questions ──────────────────────────────────────

    @GetMapping("/questions")
    public Mono<ApiResponse<List<Map<String, Object>>>> getQuestions(Authentication auth) {
        return requireTeacher(auth).flatMap(chatService::teacherCourseIds).flatMap(courseIds -> {
            if (courseIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));

            return lessonRepository.findByCourseIdInOrdered(courseIds).collectMap(Lesson::getId).flatMap(lessons -> {
                if (lessons.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));

                return teacherQuestionRepository.findByLessonIdIn(lessons.keySet()).collectList().flatMap(questions -> {
                    Set<Long> studentIds = questions.stream().map(TeacherQuestion::getUserId).collect(Collectors.toSet());
                    return userRepository.findAllById(studentIds).collectMap(BaseUser::getId).map(students -> {
                        questions.sort(Comparator.comparing(TeacherQuestion::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())));
                        List<Map<String, Object>> result = new ArrayList<>();
                        for (TeacherQuestion tq : questions) {
                            Lesson lesson = lessons.get(tq.getLessonId());
                            Map<String, Object> row = new HashMap<>();
                            row.put("id", tq.getId());
                            row.put("userId", tq.getUserId());
                            row.put("lessonId", tq.getLessonId());
                            row.put("courseId", lesson != null ? lesson.getCourseId() : null);
                            row.put("question", tq.getQuestion());
                            row.put("studentName", fullName(students.get(tq.getUserId())));
                            row.put("lessonName", lesson != null ? lesson.getName() : "");
                            row.put("createdAt", tq.getCreatedAt());
                            result.add(row);
                        }
                        return ApiResponse.ok(result);
                    });
                });
            });
        });
    }

    // ── Chat Rooms ─────────────────────────────────────────────

    @GetMapping("/chat/rooms")
    public Mono<ApiResponse<List<Map<String, Object>>>> getChatRooms(Authentication auth) {
        return requireTeacher(auth).flatMap(chatService::teacherCourseIds).flatMap(courseIds -> {
            if (courseIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));
            return buildRooms(courseIds).map(ApiResponse::ok);
        });
    }

    @GetMapping("/chat/history")
    public Mono<ApiResponse<List<ChatMessageDto>>> getChatHistory(Authentication auth, @RequestParam Long courseId, @RequestParam Long studentId) {
        // Faqat shu kursning ustozi o'qiy oladi (avval istalgan ustoz istalgan yozishmani ocha olardi)
        return requireTeacher(auth)
                .flatMap(teacherId -> chatService.isTeacherOfCourse(courseId, teacherId))
                .flatMap(allowed -> allowed
                        ? chatService.getHistory(courseId, studentId).collectList().map(ApiResponse::ok)
                        : Mono.error(new ForbiddenException("Bu kurs sizga biriktirilmagan")));
    }

    /**
     * Ustoz kurslaridagi shaxsiy chat xonalari: kursga yozilgan har bir o'quvchi + kursga
     * yozilmagan bo'lsa ham xabar yozganlar. Oxirgi xabar bilan; avval javob kutayotganlar,
     * so'ng eng yangi yozishmalar, oxirida hali yozishmagan o'quvchilar.
     */
    private Mono<List<Map<String, Object>>> buildRooms(List<Long> courseIds) {
        return Mono.zip(
                courseRepository.findAllById(courseIds).collectMap(Course::getId),
                userCourseRepository.findByCourseIdIn(courseIds).collectList(),
                chatMessageRepository.findLastPerRoom(courseIds).collectList()
        ).flatMap(t -> {
            Map<Long, Course> courses = t.getT1();
            Map<String, ChatMessage> lastByRoom = new HashMap<>();
            for (ChatMessage m : t.getT3()) lastByRoom.put(m.getCourseId() + ":" + m.getStudentId(), m);

            // xona kaliti -> {courseId, studentId}; tartib saqlanadi
            Map<String, long[]> rooms = new LinkedHashMap<>();
            Set<String> enrolled = new HashSet<>();
            for (UserCourse uc : t.getT2()) {
                if (GuestUtil.isGuest(uc.getUserId())) continue;
                String key = uc.getCourseId() + ":" + uc.getUserId();
                enrolled.add(key);
                rooms.putIfAbsent(key, new long[]{uc.getCourseId(), uc.getUserId()});
            }
            for (ChatMessage m : t.getT3()) {
                rooms.putIfAbsent(m.getCourseId() + ":" + m.getStudentId(), new long[]{m.getCourseId(), m.getStudentId()});
            }
            Set<Long> studentIds = rooms.values().stream().map(r -> r[1]).collect(Collectors.toSet());
            if (studentIds.isEmpty()) return Mono.just(Collections.<Map<String, Object>>emptyList());

            return userRepository.findAllById(studentIds).collectMap(BaseUser::getId).map(students -> {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map.Entry<String, long[]> e : rooms.entrySet()) {
                    Course course = courses.get(e.getValue()[0]);
                    BaseUser student = students.get(e.getValue()[1]);
                    if (course == null || student == null) continue;
                    ChatMessage last = lastByRoom.get(e.getKey());

                    Map<String, Object> room = new HashMap<>();
                    room.put("courseId", course.getId());
                    room.put("studentId", student.getId());
                    room.put("studentName", fullName(student));
                    room.put("courseName", course.getName());
                    room.put("enrolled", enrolled.contains(e.getKey()));
                    room.put("lastMessage", last != null ? preview(last) : null);
                    room.put("lastTime", last != null && last.getCreatedAt() != null ? last.getCreatedAt().toString() : null);
                    room.put("lastSenderRole", last != null ? last.getSenderRole() : null);
                    room.put("awaitingReply", last != null && !"teacher".equalsIgnoreCase(last.getSenderRole()));
                    result.add(room);
                }
                result.sort(Comparator
                        .comparing((Map<String, Object> r) -> !Boolean.TRUE.equals(r.get("awaitingReply")))
                        .thenComparing(r -> (String) r.get("lastTime"), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(r -> String.valueOf(r.get("studentName")).toLowerCase()));
                return result;
            });
        });
    }

    // ── Helper ──────────────────────────────────────────────────

    private static String preview(ChatMessage m) {
        String text = m.getText() != null ? m.getText().trim() : "";
        if (!text.isEmpty()) {
            return text.length() > PREVIEW_LENGTH ? text.substring(0, PREVIEW_LENGTH) + "…" : text;
        }
        if ("image".equals(m.getMediaType())) return "📷 Rasm";
        return m.getMediaPath() != null ? "📎 Fayl" : "";
    }

    private static String fullName(BaseUser user) {
        if (user == null) return "";
        String fn = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String ln = user.getLastName() != null ? user.getLastName().trim() : "";
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? "Foydalanuvchi" : full;
    }

    /** Rol DB'dan tekshiriladi: faqat TEACHER yoki ADMIN kiradi. */
    private Mono<Long> requireTeacher(Authentication auth) {
        return roleGuard.requireTeacherOrAdmin(auth);
    }
}
