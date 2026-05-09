package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;

public interface LessonRepository extends ReactiveCrudRepository<Lesson, Long> {
    Flux<Lesson> findByCourseIdOrderByOrderIndex(Long courseId);

    @Query("SELECT COUNT(*) FROM lessons WHERE course_id = :courseId")
    Mono<Long> countByCourseId(Long courseId);

    @Query("SELECT COALESCE(SUM(duration_sec), 0) FROM lessons WHERE course_id = :courseId")
    Mono<Long> sumDurationByCourseId(Long courseId);
}
