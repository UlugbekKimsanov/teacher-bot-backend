package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.lesson.entity.Audiobook;

public interface AudiobookRepository extends ReactiveCrudRepository<Audiobook, Long> {
    Flux<Audiobook> findByLessonIdOrderByOrderIndex(Long lessonId);
}
