package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.sevenEdu.teacherBot.lesson.entity.Vocabulary;

import java.util.List;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    List<Vocabulary> findByLessonIdOrderByOrderIndex(Long lessonId);
}
