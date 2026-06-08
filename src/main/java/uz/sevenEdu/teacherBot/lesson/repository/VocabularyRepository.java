package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.lesson.entity.Vocabulary;

public interface VocabularyRepository extends ReactiveCrudRepository<Vocabulary, Long> {
    Flux<Vocabulary> findByLessonIdOrderByOrderIndex(Long lessonId);

    /** O'rganilgan so'zlar soni: vocab_score >= 50% bo'lgan darslardagi barcha so'zlar */
    @Query("SELECT COUNT(*) FROM vocabulary v " +
           "JOIN user_lessons ul ON v.lesson_id = ul.lesson_id " +
           "WHERE ul.user_id = :userId AND ul.vocab_score IS NOT NULL AND ul.vocab_score >= 50")
    Mono<Long> countLearnedByUserId(Long userId);

    /** Bugun yakunlangan darslardagi so'zlar soni */
    @Query("SELECT COUNT(*) FROM vocabulary v " +
           "JOIN user_lessons ul ON v.lesson_id = ul.lesson_id " +
           "WHERE ul.user_id = :userId AND ul.is_completed = true " +
           "AND ul.completed_at >= CURRENT_DATE")
    Mono<Long> countTodayLearnedByUserId(Long userId);
}
