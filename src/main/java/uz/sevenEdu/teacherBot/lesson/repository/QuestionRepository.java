package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.sevenEdu.teacherBot.lesson.entity.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query(value = "SELECT q.* FROM questions q JOIN tests t ON q.test_id = t.id WHERE t.lesson_id = :lessonId ORDER BY q.order_index", nativeQuery = true)
    List<Question> findByLessonId(Long lessonId);
}
