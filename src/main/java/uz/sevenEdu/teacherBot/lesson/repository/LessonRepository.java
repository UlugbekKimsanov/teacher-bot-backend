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

    /** Bir nechta kurs darslarini bitta so'rovda (N+1 o'rniga batch). */
    @Query("SELECT * FROM lessons WHERE course_id IN (:courseIds) ORDER BY course_id ASC, order_index ASC")
    Flux<Lesson> findByCourseIdInOrdered(java.util.Collection<Long> courseIds);

    @Query("SELECT * FROM lessons WHERE course_id = :courseId AND order_index > :orderIndex ORDER BY order_index ASC LIMIT 1")
    Mono<Lesson> findNextLessonInCourse(Long courseId, Integer orderIndex);
}
