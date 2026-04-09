package uz.sevenEdu.teacherBot.course.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;

public interface CourseService {
    Flux<CourseDto> getAllCourses(Long userId);
    Flux<CourseDto> getCoursesByCategory(String category, Long userId);
    Mono<CourseDto> getCourseById(Long courseId, Long userId);
    Mono<CourseDto> enrollCourse(Long userId, Long courseId);
}
