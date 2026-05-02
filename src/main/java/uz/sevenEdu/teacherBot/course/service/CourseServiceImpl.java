package uz.sevenEdu.teacherBot.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.entity.UserCourse;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.LanguageRepository;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final LanguageRepository languageRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Flux<CourseDto> getAllCourses(Long userId) {
        return courseRepository.findAll()
                .flatMap(course -> enrichWithEnrollment(course, userId));
    }

    @Override
    public Flux<CourseDto> getCoursesByCategory(String category, Long userId) {
        return courseRepository.findByCategory(category)
                .flatMap(course -> enrichWithEnrollment(course, userId));
    }

    @Override
    public Mono<CourseDto> getCourseById(Long courseId, Long userId) {
        return courseRepository.findById(courseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course -> enrichWithEnrollment(course, userId));
    }

    @Override
    public Mono<CourseDto> enrollCourse(Long userId, Long courseId) {
        return courseRepository.findById(courseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course ->
                        userCourseRepository.existsByUserIdAndCourseId(userId, courseId)
                                .flatMap(exists -> {
                                    if (exists) {
                                        return userCourseRepository
                                                .findByUserIdAndCourseId(userId, courseId)
                                                .flatMap(uc -> enrichWithEnrollment(course, userId));
                                    }
                                    UserCourse uc = UserCourse.builder()
                                            .userId(userId)
                                            .courseId(courseId)
                                            .progress(BigDecimal.ZERO)
                                            .createdAt(LocalDateTime.now())
                                            .build();
                                    return userCourseRepository.save(uc)
                                            .flatMap(saved -> enrichWithEnrollment(course, userId));
                                })
                );
    }

    @Override
    public Mono<Course> create(Long languageId, String name, String category,
                               Integer hours, Integer lessonCount, String goal, Boolean isPremium) {
        return languageRepository.findById(languageId)
                .switchIfEmpty(Mono.error(new NotFoundException("Til topilmadi")))
                .flatMap(language -> {
                    Course course = Course.builder()
                            .name(name)
                            .category(category)
                            .languageId(languageId)
                            .hours(hours != null ? hours : 0)
                            .lessonCount(lessonCount != null ? lessonCount : 0)
                            .goal(goal)
                            .isPremium(isPremium != null ? isPremium : true)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return courseRepository.save(course)
                            .doOnNext(saved -> fileStorageService.createCourseFolder(
                                    language.getName(), language.getId(),
                                    saved.getName(), saved.getId()));
                });
    }

    @Override
    public Mono<Course> uploadCoverImage(Long courseId, FilePart filePart) {
        return courseRepository.findById(courseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course -> languageRepository.findById(course.getLanguageId())
                        .switchIfEmpty(Mono.error(new NotFoundException("Til topilmadi")))
                        .flatMap(language -> fileStorageService
                                .saveCourseImage(language.getName(), language.getId(),
                                        course.getName(), course.getId(), filePart, course.getCoverImage())
                                .flatMap(path -> {
                                    course.setCoverImage(path);
                                    return courseRepository.save(course);
                                })));
    }

    private Mono<CourseDto> enrichWithEnrollment(Course course, Long userId) {
        if (userId == null) {
            return Mono.just(toDto(course, false, BigDecimal.ZERO));
        }
        return userCourseRepository.findByUserIdAndCourseId(userId, course.getId())
                .map(uc -> toDto(course, true, uc.getProgress()))
                .defaultIfEmpty(toDto(course, false, BigDecimal.ZERO));
    }

    private CourseDto toDto(Course c, boolean enrolled, BigDecimal progress) {
        return CourseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .category(c.getCategory())
                .flagEmoji(c.getFlagEmoji())
                .hours(c.getHours())
                .lessonCount(c.getLessonCount())
                .goal(c.getGoal())
                .isPremium(c.getIsPremium())
                .isEnrolled(enrolled)
                .progress(progress)
                .build();
    }
}
