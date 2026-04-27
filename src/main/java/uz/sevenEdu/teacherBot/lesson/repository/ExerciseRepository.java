package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.sevenEdu.teacherBot.lesson.entity.Exercise;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    List<Exercise> findByLessonIdOrderByOrderIndex(Long lessonId);
}
