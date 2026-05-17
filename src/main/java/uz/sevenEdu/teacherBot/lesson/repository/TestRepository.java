package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.lesson.entity.TestEntity;

public interface TestRepository extends ReactiveCrudRepository<TestEntity, Long> {
    Mono<TestEntity> findByLessonId(Long lessonId);
}
