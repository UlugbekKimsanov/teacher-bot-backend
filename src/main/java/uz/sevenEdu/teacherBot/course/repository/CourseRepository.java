package uz.sevenEdu.teacherBot.course.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.course.entity.Course;

public interface CourseRepository extends ReactiveCrudRepository<Course, Long> {
    Flux<Course> findBySubjectId(Long subjectId);
    Flux<Course> findByLanguageId(Long languageId);
}
