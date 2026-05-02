package uz.sevenEdu.teacherBot.course.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.entity.UserCourse;

public interface UserCourseRepository extends ReactiveCrudRepository<UserCourse, Long> {
    Flux<UserCourse> findByUserId(Long userId);
    Mono<UserCourse> findByUserIdAndCourseId(Long userId, Long courseId);
    Mono<Boolean> existsByUserIdAndCourseId(Long userId, Long courseId);
}
