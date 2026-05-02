package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.lesson.entity.Exercise;

public interface ExerciseRepository extends ReactiveCrudRepository<Exercise, Long> {
    Flux<Exercise> findByLessonIdOrderByOrderIndex(Long lessonId);
}
