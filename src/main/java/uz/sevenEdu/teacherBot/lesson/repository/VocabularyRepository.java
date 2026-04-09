package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.lesson.entity.Vocabulary;

public interface VocabularyRepository extends ReactiveCrudRepository<Vocabulary, Long> {
    Flux<Vocabulary> findByLessonIdOrderByOrderIndex(Long lessonId);
}
