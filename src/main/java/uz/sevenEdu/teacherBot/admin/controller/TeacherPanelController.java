package uz.sevenEdu.teacherBot.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.chat.entity.ChatMessage;
import uz.sevenEdu.teacherBot.chat.repository.ChatMessageRepository;
import uz.sevenEdu.teacherBot.chat.repository.CourseTeacherRepository;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.entity.UserCourse;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.lesson.entity.TeacherQuestion;
import uz.sevenEdu.teacherBot.lesson.repository.LessonRepository;
import uz.sevenEdu.teacherBot.lesson.repository.TeacherQuestionRepository;
import uz.sevenEdu.teacherBot.lesson.repository.UserLessonRepository;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherPanelController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserCourseRepository userCourseRepository;
    private final LessonRepository lessonRepository;
    private final UserLessonRepository userLessonRepository;
    private final TeacherQuestionRepository teacherQuestionRepository;
    private final ChatMessageRepository chatMessageRepository;

    // ── Dashboard ──────────────────────────────────────────────

    @GetMapping("/dashboard")
    public Mono<ApiResponse<Map<String, Object>>> dashboard(Authentication auth) {
        Long teacherId = requireTeacher(auth);
        return courseTeacherRepository.findByTeacherId(teacherId)
                .collectList()
                .flatMap(courseTeachers -> {
                    List<Long> courseIds = courseTeachers.stream().map(ct -> ct.getCourseId()).collect(Collectors.toList());
                    if (courseIds.isEmpty()) {
                        Map<String, Object> stats = new HashMap<>();
                        stats.put("myCourses", 0);
                        stats.put("myStudents", 0);
                        stats.put("pendingQuestions", 0);
                        stats.put("unreadChats", 0);
                        return Mono.just(ApiResponse.ok(stats));
                    }
                    return Mono.zip(
                            Mono.just(courseIds.size()),
                            userCourseRepository.findByCourseIdIn(courseIds).map(UserCourse::getUserId).distinct().count(),
                            lessonRepository.findAll().filter(l -> courseIds.contains(l.getCourseId())).map(l -> l.getId()).collectList()
                                    .flatMap(lessonIds -> lessonIds.isEmpty()
                                            ? Mono.just(0L)
                                            : teacherQuestionRepository.findByLessonIdIn(lessonIds).count())
                    ).map(t -> {
                        Map<String, Object> stats = new HashMap<>();
                        stats.put("myCourses", t.getT1());
                        stats.put("myStudents", t.getT2());
                        stats.put("pendingQuestions", t.getT3());
                        stats.put("unreadChats", 0);
                        return ApiResponse.ok(stats);
                    });
                });
    }

    // ── My Courses ─────────────────────────────────────────────

    @GetMapping("/courses")
    public Mono<ApiResponse<List<Course>>> getMyCourses(Authentication auth) {
        Long teacherId = requireTeacher(auth);
        return courseTeacherRepository.findByTeacherId(teacherId)
                .flatMap(ct -> courseRepository.findById(ct.getCourseId()))
                .collectList()
                .map(ApiResponse::ok);
    }

    // ── My Students ────────────────────────────────────────────

    @GetMapping("/students")
    public Mono<ApiResponse<List<Map<String, Object>>>> getMyStudents(Authentication auth) {
        Long teacherId = requireTeacher(auth);
        return courseTeacherRepository.findByTeacherId(teacherId)
                .collectList()
                .flatMap(courseTeachers -> {
                    List<Long> courseIds = courseTeachers.stream().map(ct -> ct.getCourseId()).collect(Collectors.toList());
                    if (courseIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));

                    return Mono.zip(
                            courseRepository.findAll().filter(c -> courseIds.contains(c.getId())).collectList(),
                            userCourseRepository.findByCourseIdIn(courseIds).collectList()
                    ).flatMap(t -> {
                        List<Course> courses = t.getT1();
                        List<UserCourse> userCourses = t.getT2();
                        Set<Long> studentIds = userCourses.stream().map(UserCourse::getUserId).collect(Collectors.toSet());

                        return userRepository.findAllById(studentIds).collectList()
                                .flatMap(students -> {
                                    // Count total lessons per course
                                    return lessonRepository.findAll().filter(l -> courseIds.contains(l.getCourseId())).collectList()
                                            .flatMap(lessons -> userLessonRepository.findAll().collectList()
                                                    .map(userLessons -> {
                                                        List<Map<String, Object>> result = new ArrayList<>();
                                                        for (UserCourse uc : userCourses) {
                                                            BaseUser student = students.stream().filter(s -> s.getId().equals(uc.getUserId())).findFirst().orElse(null);
                                                            Course course = courses.stream().filter(c -> c.getId().equals(uc.getCourseId())).findFirst().orElse(null);
                                                            if (student == null || course == null) continue;

                                                            long totalLessons = lessons.stream().filter(l -> l.getCourseId().equals(uc.getCourseId())).count();
                                                            long completedLessons = userLessons.stream()
                                                                    .filter(ul -> ul.getUserId().equals(uc.getUserId())
                                                                            && Boolean.TRUE.equals(ul.getIsCompleted())
                                                                            && lessons.stream().anyMatch(l -> l.getId().equals(ul.getLessonId()) && l.getCourseId().equals(uc.getCourseId())))
                                                                    .count();

                                                            Map<String, Object> row = new HashMap<>();
                                                            row.put("id", student.getId());
                                                            row.put("userId", student.getId());
                                                            row.put("firstName", student.getFirstName());
                                                            row.put("lastName", student.getLastName());
                                                            row.put("courseId", course.getId());
                                                            row.put("courseName", course.getName());
                                                            row.put("totalLessons", totalLessons);
                                                            row.put("lessonsCompleted", completedLessons);
                                                            row.put("progress", totalLessons > 0 ? Math.round(completedLessons * 100.0 / totalLessons) : 0);
                                                            result.add(row);
                                                        }
                                                        return ApiResponse.ok(result);
                                                    }));
                                });
                    });
                });
    }

    // ── Teacher Questions ──────────────────────────────────────

    @GetMapping("/questions")
    public Mono<ApiResponse<List<Map<String, Object>>>> getQuestions(Authentication auth) {
        Long teacherId = requireTeacher(auth);
        return courseTeacherRepository.findByTeacherId(teacherId)
                .collectList()
                .flatMap(courseTeachers -> {
                    List<Long> courseIds = courseTeachers.stream().map(ct -> ct.getCourseId()).collect(Collectors.toList());
                    if (courseIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));

                    return lessonRepository.findAll().filter(l -> courseIds.contains(l.getCourseId())).collectList()
                            .flatMap(lessons -> {
                                List<Long> lessonIds = lessons.stream().map(l -> l.getId()).collect(Collectors.toList());
                                if (lessonIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));

                                return teacherQuestionRepository.findByLessonIdIn(lessonIds).collectList()
                                        .flatMap(questions -> {
                                            Set<Long> studentIds = questions.stream().map(TeacherQuestion::getUserId).collect(Collectors.toSet());
                                            return userRepository.findAllById(studentIds).collectList()
                                                    .map(students -> {
                                                        List<Map<String, Object>> result = new ArrayList<>();
                                                        for (TeacherQuestion tq : questions) {
                                                            BaseUser student = students.stream().filter(s -> s.getId().equals(tq.getUserId())).findFirst().orElse(null);
                                                            var lesson = lessons.stream().filter(l -> l.getId().equals(tq.getLessonId())).findFirst().orElse(null);
                                                            Map<String, Object> row = new HashMap<>();
                                                            row.put("id", tq.getId());
                                                            row.put("userId", tq.getUserId());
                                                            row.put("lessonId", tq.getLessonId());
                                                            row.put("question", tq.getQuestion());
                                                            row.put("studentName", student != null ? student.getFirstName() + " " + student.getLastName() : "");
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
        Long teacherId = requireTeacher(auth);
        return courseTeacherRepository.findByTeacherId(teacherId)
                .collectList()
                .flatMap(courseTeachers -> {
                    List<Long> courseIds = courseTeachers.stream().map(ct -> ct.getCourseId()).collect(Collectors.toList());
                    if (courseIds.isEmpty()) return Mono.just(ApiResponse.ok(Collections.<Map<String, Object>>emptyList()));

                    return Mono.zip(
                            courseRepository.findAll().filter(c -> courseIds.contains(c.getId())).collectList(),
                            userCourseRepository.findByCourseIdIn(courseIds).collectList()
                    ).flatMap(t -> {
                        List<Course> courses = t.getT1();
                        List<UserCourse> userCourses = t.getT2();
                        Set<Long> studentIds = userCourses.stream().map(UserCourse::getUserId).collect(Collectors.toSet());

                        return userRepository.findAllById(studentIds).collectList().map(students -> {
                            List<Map<String, Object>> rooms = new ArrayList<>();
                            for (UserCourse uc : userCourses) {
                                BaseUser student = students.stream().filter(s -> s.getId().equals(uc.getUserId())).findFirst().orElse(null);
                                Course course = courses.stream().filter(c -> c.getId().equals(uc.getCourseId())).findFirst().orElse(null);
                                if (student == null || course == null) continue;

                                Map<String, Object> room = new HashMap<>();
                                room.put("courseId", course.getId());
                                room.put("studentId", student.getId());
                                room.put("studentName", student.getFirstName() + " " + student.getLastName());
                                room.put("courseName", course.getName());
                                rooms.add(room);
                            }
                            return ApiResponse.ok(rooms);
                        });
                    });
                });
    }

    @GetMapping("/chat/history")
    public Mono<ApiResponse<List<ChatMessage>>> getChatHistory(Authentication auth, @RequestParam Long courseId, @RequestParam Long studentId) {
        requireTeacher(auth);
        return chatMessageRepository.findByCourseAndStudent(courseId, studentId)
                .collectList()
                .map(ApiResponse::ok);
    }

    // ── Helper ──────────────────────────────────────────────────

    private Long requireTeacher(Authentication auth) {
        if (auth == null) throw new RuntimeException("Unauthorized");
        return (Long) auth.getPrincipal();
    }
}
