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
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;

    @Override
    public Flux<CourseDto> getAllCourses(Long userId) {
        return courseRepository.findAll().flatMap(course -> enrichWithEnrollment(course, userId));
    }

    @Override
    public Flux<CourseDto> getCoursesByCategory(String category, Long userId) {
        return Flux.empty();
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
                .flatMap(course -> userCourseRepository.existsByUserIdAndCourseId(userId, courseId)
                        .flatMap(exists -> {
                            if (exists) return Mono.just(course);
                            UserCourse uc = UserCourse.builder()
                                    .userId(userId).courseId(courseId)
                                    .progress(BigDecimal.ZERO).createdAt(LocalDateTime.now()).build();
                            return userCourseRepository.save(uc).thenReturn(course);
                        }))
                .flatMap(course -> enrichWithEnrollment(course, userId));
    }

    private Mono<CourseDto> enrichWithEnrollment(Course course, Long userId) {
        if (userId == null) return Mono.just(toDto(course, false, BigDecimal.ZERO));
        return userCourseRepository.findByUserIdAndCourseId(userId, course.getId())
                .map(uc -> toDto(course, true, uc.getProgress()))
                .defaultIfEmpty(toDto(course, false, BigDecimal.ZERO));
    }

    private CourseDto toDto(Course c, boolean enrolled, BigDecimal progress) {
        return CourseDto.builder().id(c.getId()).name(c.getName())
                .imageUrl(c.getCoverImage()).isEnrolled(enrolled).progress(progress).build();
    }
}
