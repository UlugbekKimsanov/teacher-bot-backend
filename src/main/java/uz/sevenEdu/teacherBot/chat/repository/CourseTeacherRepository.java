package uz.sevenEdu.teacherBot.chat.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.chat.entity.CourseTeacher;

public interface CourseTeacherRepository extends ReactiveCrudRepository<CourseTeacher, Long> {

    Flux<CourseTeacher> findByCourseId(Long courseId);

    Flux<CourseTeacher> findByTeacherId(Long teacherId);

    Mono<CourseTeacher> findByCourseIdAndTeacherId(Long courseId, Long teacherId);
}
