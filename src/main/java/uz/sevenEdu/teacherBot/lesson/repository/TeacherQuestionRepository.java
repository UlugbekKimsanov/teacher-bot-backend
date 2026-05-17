package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.lesson.entity.TeacherQuestion;

public interface TeacherQuestionRepository extends ReactiveCrudRepository<TeacherQuestion, Long> {
    Flux<TeacherQuestion> findByLessonIdIn(java.util.Collection<Long> lessonIds);
}
