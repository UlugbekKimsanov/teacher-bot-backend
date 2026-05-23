package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.Achievement;

public interface AchievementRepository extends ReactiveCrudRepository<Achievement, Long> {
    Flux<Achievement> findAll();
    Mono<Achievement> findByCode(String code);
}
