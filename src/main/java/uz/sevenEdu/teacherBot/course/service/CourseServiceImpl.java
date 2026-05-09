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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final LanguageRepository languageRepository;
    private final LessonRepository lessonRepository;

    @Override
    public Flux<CourseDto> getAllCourses(Long userId) {
        return courseRepository.findAll().flatMap(course -> enrichCourse(course, userId));
    }

    @Override
    public Flux<CourseDto> getCoursesByCategory(String category, Long userId) {
        return languageRepository.findByName(category)
                .flatMapMany(lang -> courseRepository.findByLanguageId(lang.getId()))
                .flatMap(course -> enrichCourse(course, userId));
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
                .flatMap(course -> userCourseRepository.existsByUserIdAndCourseId(userId, courseId)
                        .flatMap(exists -> {
                            if (exists) return Mono.just(course);
                            UserCourse uc = UserCourse.builder()
                                    .userId(userId).courseId(courseId)
                                    .progress(BigDecimal.ZERO).createdAt(LocalDateTime.now()).build();
                            return userCourseRepository.save(uc).thenReturn(course);
                        }))
                .flatMap(course -> enrichCourse(course, userId));
    }

    private Mono<CourseDto> enrichCourse(Course course, Long userId) {
        Mono<String> categoryMono = course.getLanguageId() != null
                ? languageRepository.findById(course.getLanguageId())
                        .map(lang -> lang.getName())
                        .defaultIfEmpty("")
                : Mono.just("");

        Mono<Long> lessonCountMono = lessonRepository.countByCourseId(course.getId());
        Mono<Long> durationMono = lessonRepository.sumDurationByCourseId(course.getId());

        Mono<Map.Entry<Boolean, BigDecimal>> enrollMono = userId != null
                ? userCourseRepository.findByUserIdAndCourseId(userId, course.getId())
                        .map(uc -> (Map.Entry<Boolean, BigDecimal>) new AbstractMap.SimpleEntry<>(true, uc.getProgress()))
                        .defaultIfEmpty(new AbstractMap.SimpleEntry<>(false, BigDecimal.ZERO))
                : Mono.just(new AbstractMap.SimpleEntry<>(false, BigDecimal.ZERO));

        return Mono.zip(categoryMono, lessonCountMono, durationMono, enrollMono)
                .map(tuple -> {
                    String category = tuple.getT1();
                    int lessonCount = tuple.getT2().intValue();
                    long totalSecs = tuple.getT3();
                    int hours = totalSecs > 0 ? (int) Math.ceil(totalSecs / 3600.0) : 0;
                    boolean enrolled = tuple.getT4().getKey();
                    BigDecimal progress = tuple.getT4().getValue();
                    return toDto(course, category, enrolled, progress, lessonCount, hours);
                });
    }

    private CourseDto toDto(Course c, String category, boolean enrolled, BigDecimal progress, int lessonCount, int hours) {
        return CourseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .category(category)
                .imageUrl(c.getCoverImage())
                .flagEmoji(c.getFlagEmoji())
                .goal(c.getGoal())
                .isPremium(c.getIsPremium() != null ? c.getIsPremium() : true)
                .isEnrolled(enrolled)
                .progress(progress)
                .lessonCount(lessonCount)
                .hours(hours)
                .build();
    }
}
