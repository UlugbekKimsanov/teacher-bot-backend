package uz.sevenEdu.teacherBot.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.entity.UserCourse;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;

    @Override
    public List<CourseDto> getAllCourses(Long userId) {
        return courseRepository.findAll().stream()
                .map(course -> enrichWithEnrollment(course, userId))
                .toList();
    }

    @Override
    public CourseDto getCourseById(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Kurs topilmadi"));
        return enrichWithEnrollment(course, userId);
    }

    @Override
    public CourseDto enrollCourse(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Kurs topilmadi"));
        if (!userCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
            UserCourse uc = UserCourse.builder()
                    .userId(userId)
                    .courseId(courseId)
                    .progress(BigDecimal.ZERO)
                    .enrolledAt(LocalDateTime.now())
                    .build();
            userCourseRepository.save(uc);
        }
        return enrichWithEnrollment(course, userId);
    }

    private CourseDto enrichWithEnrollment(Course course, Long userId) {
        if (userId == null) {
            return toDto(course, false, BigDecimal.ZERO);
        }
        return userCourseRepository.findByUserIdAndCourseId(userId, course.getId())
                .map(uc -> toDto(course, true, uc.getProgress()))
                .orElse(toDto(course, false, BigDecimal.ZERO));
    }

    private CourseDto toDto(Course c, boolean enrolled, BigDecimal progress) {
        return CourseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .imageUrl(c.getImage() != null ? c.getImage().getPath() : null)
                .isEnrolled(enrolled)
                .progress(progress)
                .build();
    }
}
