package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import uz.sevenEdu.teacherBot.lesson.entity.TeacherQuestion;

public interface TeacherQuestionRepository extends ReactiveCrudRepository<TeacherQuestion, Long> {}
