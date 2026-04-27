package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.sevenEdu.teacherBot.lesson.entity.TeacherQuestion;

public interface TeacherQuestionRepository extends JpaRepository<TeacherQuestion, Long> {
}
