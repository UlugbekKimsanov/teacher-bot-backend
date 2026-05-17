package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.lesson.entity.Question;

public interface QuestionRepository extends ReactiveCrudRepository<Question, Long> {
    @Query("SELECT q.* FROM questions q JOIN tests t ON q.test_id = t.id WHERE t.lesson_id = :lessonId ORDER BY q.order_index")
    Flux<Question> findByLessonId(Long lessonId);

    Flux<Question> findByTestIdOrderByOrderIndex(Long testId);
}
