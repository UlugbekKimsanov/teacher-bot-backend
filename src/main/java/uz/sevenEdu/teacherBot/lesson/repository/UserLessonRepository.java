package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.lesson.entity.UserLesson;

public interface UserLessonRepository extends ReactiveCrudRepository<UserLesson, Long> {
    Mono<UserLesson> findByUserIdAndLessonId(Long userId, Long lessonId);
    Flux<UserLesson> findByUserId(Long userId);

    @Query("SELECT ul.* FROM user_lessons ul " +
           "JOIN lessons l ON ul.lesson_id = l.id " +
           "WHERE ul.user_id = :userId AND l.course_id = :courseId")
    Flux<UserLesson> findByUserIdAndCourseId(Long userId, Long courseId);
}
