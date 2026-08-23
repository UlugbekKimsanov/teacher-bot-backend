package uz.sevenEdu.teacherBot.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.entity.UserCourse;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.LanguageRepository;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.lesson.repository.LessonRepository;

import uz.sevenEdu.teacherBot.lesson.entity.Lesson;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final LanguageRepository languageRepository;
    private final LessonRepository lessonRepository;
    private final uz.sevenEdu.teacherBot.lesson.repository.UserLessonRepository userLessonRepository;
    private final uz.sevenEdu.teacherBot.common.service.FileStorageService fileStorageService;

    // Til xaritasi (id -> nom) — statik, har kurs-ro'yxat so'rovida DB urilmasligi uchun
    // 10 daqiqaga keshlanadi (Reactor cache, reaktiv-xavfsiz).
    private volatile Mono<Map<Long, String>> langCache;

    private Mono<Map<Long, String>> languageMap() {
        Mono<Map<Long, String>> c = langCache;
        if (c == null) {
            c = languageRepository.findAll()
                    .collectMap(l -> l.getId(), l -> l.getName() != null ? l.getName() : "")
                    .cache(java.time.Duration.ofMinutes(10));
            langCache = c;
        }
        return c;
    }

    @Override
    public Flux<CourseDto> getAllCourses(Long userId) {
        return courseRepository.findAll().collectList()
                .flatMapMany(courses -> enrichCoursesBatch(courses, userId));
    }

    @Override
    public Flux<CourseDto> getCoursesByCategory(String category, Long userId) {
        return languageRepository.findByName(category)
                .flatMapMany(lang -> courseRepository.findByLanguageId(lang.getId()))
                .collectList()
                .flatMapMany(courses -> {
                    // Admin sozlagan tartib (order_index), yo'q bo'lsa yaratilish tartibi (id)
                    courses.sort((a, b) -> Integer.compare(
                            a.getOrderIndex() != null ? a.getOrderIndex() : a.getId().intValue(),
                            b.getOrderIndex() != null ? b.getOrderIndex() : b.getId().intValue()));
                    return enrichCoursesBatch(courses, userId);
                });
    }

    @Override
    public Mono<CourseDto> getCourseById(Long courseId, Long userId) {
        return courseRepository.findById(courseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course -> enrichCourse(course, userId));
    }

    @Override
    public Mono<CourseDto> enrollCourse(Long userId, Long courseId) {
        return courseRepository.findById(courseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course -> {
                    // Mehmon — kursga yozilmaydi (progress/enrollment qayd etilmaydi)
                    if (uz.sevenEdu.teacherBot.common.util.GuestUtil.isGuest(userId)) {
                        return Mono.just(course);
                    }
                    return userCourseRepository.existsByUserIdAndCourseId(userId, courseId)
                            .flatMap(exists -> {
                                if (exists) return Mono.just(course);
                                UserCourse uc = UserCourse.builder()
                                        .userId(userId).courseId(courseId)
                                        .progress(BigDecimal.ZERO).createdAt(LocalDateTime.now()).build();
                                return userCourseRepository.save(uc).thenReturn(course);
                            });
                })
                .flatMap(course -> enrichCourse(course, userId));
    }

    private Mono<CourseDto> enrichCourse(Course course, Long userId) {
        Mono<String> categoryMono = course.getLanguageId() != null
                ? languageRepository.findById(course.getLanguageId())
                        .map(lang -> lang.getName())
                        .defaultIfEmpty("")
                : Mono.just("");

        Mono<Long> durationMono = lessonRepository.sumDurationByCourseId(course.getId());

        Mono<Boolean> enrolledMono = userId != null
                ? userCourseRepository.existsByUserIdAndCourseId(userId, course.getId())
                : Mono.just(false);

        // Barcha darslarni tartib bo'yicha olish
        Mono<List<Lesson>> lessonsMono = lessonRepository
                .findByCourseIdOrderByOrderIndex(course.getId()).collectList();

        // Tugatilgan dars ID lari
        Mono<Set<Long>> completedIdsMono = userId != null
                ? userLessonRepository.findByUserIdAndCourseId(userId, course.getId())
                        .filter(ul -> Boolean.TRUE.equals(ul.getIsCompleted()))
                        .map(ul -> ul.getLessonId())
                        .collect(Collectors.toSet())
                : Mono.just(Set.of());

        return Mono.zip(categoryMono, durationMono, enrolledMono, lessonsMono, completedIdsMono)
                .map(tuple -> {
                    String category = tuple.getT1();
                    long totalSecs = tuple.getT2();
                    int hours = totalSecs > 0 ? (int) Math.ceil(totalSecs / 3600.0) : 0;
                    boolean enrolled = tuple.getT3();
                    List<Lesson> lessons = tuple.getT4();
                    Set<Long> completedIds = tuple.getT5();

                    int lessonCount = lessons.size();
                    int completed = (int) lessons.stream()
                            .filter(l -> completedIds.contains(l.getId())).count();
                    BigDecimal progress = lessonCount > 0
                            ? BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(lessonCount), 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    // Hozirgi dars — birinchi tugatilmagan dars
                    Lesson cl = lessons.stream()
                            .filter(l -> !completedIds.contains(l.getId()))
                            .findFirst().orElse(null);

                    return toDto(course, category, enrolled, progress, completed, lessonCount, hours, cl);
                });
    }

    /** Kurslar ro'yxati uchun batch boyitish — N+1 (har kurs × 5 so'rov) o'rniga
     *  butun ro'yxat uchun ~4 so'rov: til xaritasi, enrollment to'plami,
     *  tugatilgan darslar to'plami, barcha darslar (IN). */
    private Flux<CourseDto> enrichCoursesBatch(List<Course> courses, Long userId) {
        if (courses.isEmpty()) return Flux.empty();
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        Mono<Map<Long, String>> langMapMono = languageMap();

        Mono<Set<Long>> enrolledMono = userId != null
                ? userCourseRepository.findByUserId(userId)
                        .map(UserCourse::getCourseId).collect(Collectors.toSet())
                : Mono.just(Set.of());

        Mono<Set<Long>> completedMono = userId != null
                ? userLessonRepository.findByUserId(userId)
                        .filter(ul -> Boolean.TRUE.equals(ul.getIsCompleted()))
                        .map(ul -> ul.getLessonId()).collect(Collectors.toSet())
                : Mono.just(Set.of());

        Mono<Map<Long, Collection<Lesson>>> lessonsMono = lessonRepository
                .findByCourseIdInOrdered(courseIds)
                .collectMultimap(Lesson::getCourseId);

        return Mono.zip(langMapMono, enrolledMono, completedMono, lessonsMono)
                .flatMapMany(t -> {
                    Map<Long, String> langs = t.getT1();
                    Set<Long> enrolled = t.getT2();
                    Set<Long> completedIds = t.getT3();
                    Map<Long, Collection<Lesson>> lessonsByCourse = t.getT4();
                    List<CourseDto> dtos = new ArrayList<>(courses.size());
                    for (Course course : courses) {
                        String category = course.getLanguageId() != null
                                ? langs.getOrDefault(course.getLanguageId(), "") : "";
                        List<Lesson> lessons = new ArrayList<>(
                                lessonsByCourse.getOrDefault(course.getId(), List.of()));
                        long totalSecs = lessons.stream()
                                .mapToLong(l -> l.getDurationSec() != null ? l.getDurationSec() : 0L).sum();
                        int hours = totalSecs > 0 ? (int) Math.ceil(totalSecs / 3600.0) : 0;
                        boolean isEnrolled = enrolled.contains(course.getId());
                        int lessonCount = lessons.size();
                        int completed = (int) lessons.stream()
                                .filter(l -> completedIds.contains(l.getId())).count();
                        BigDecimal progress = lessonCount > 0
                                ? BigDecimal.valueOf(completed).divide(
                                        BigDecimal.valueOf(lessonCount), 4, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        Lesson cl = lessons.stream()
                                .filter(l -> !completedIds.contains(l.getId()))
                                .findFirst().orElse(null);
                        dtos.add(toDto(course, category, isEnrolled, progress, completed,
                                lessonCount, hours, cl));
                    }
                    return Flux.fromIterable(dtos);
                });
    }

    private CourseDto toDto(Course c, String category, boolean enrolled, BigDecimal progress,
                            int completedLessons, int lessonCount, int hours, Lesson currentLesson) {
        return CourseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .category(category)
                .imageUrl(fileStorageService.toPublicUrl(c.getCoverImage()))
                .backgroundUrl(fileStorageService.toPublicUrl(c.getBackgroundImage()))
                .flagEmoji(c.getFlagEmoji())
                .goal(c.getGoal())
                .isPremium(c.getIsPremium() != null ? c.getIsPremium() : true)
                .price(c.getPrice())
                .priceLabel(c.getPriceLabel())
                .isEnrolled(enrolled)
                .progress(progress)
                .completedLessons(completedLessons)
                .lessonCount(lessonCount)
                .hours(hours)
                .currentLessonId(currentLesson != null ? currentLesson.getId() : null)
                .currentLessonTitle(currentLesson != null ? currentLesson.getName() : null)
                .currentLessonImage(currentLesson != null ? fileStorageService.toPublicUrl(currentLesson.getCoverImage()) : null)
                .currentLessonDurationSec(currentLesson != null ? currentLesson.getDurationSec() : null)
                .build();
    }
}
