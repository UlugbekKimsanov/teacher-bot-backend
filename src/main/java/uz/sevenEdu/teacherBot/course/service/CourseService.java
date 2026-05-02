package uz.sevenEdu.teacherBot.course.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;
import uz.sevenEdu.teacherBot.course.entity.Course;

public interface CourseService {
    Flux<CourseDto> getAllCourses(Long userId);
    Flux<CourseDto> getCoursesByCategory(String category, Long userId);
    Mono<CourseDto> getCourseById(Long courseId, Long userId);
    Mono<CourseDto> enrollCourse(Long userId, Long courseId);
    Mono<Course> create(Long languageId, String name, String category, Integer hours, Integer lessonCount, String goal, Boolean isPremium);
    Mono<Course> uploadCoverImage(Long courseId, FilePart filePart);
}
