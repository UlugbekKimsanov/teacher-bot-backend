package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;

public interface LessonRepository extends ReactiveCrudRepository<Lesson, Long> {
    Flux<Lesson> findByCourseIdOrderByOrderIndex(Long courseId);
}
