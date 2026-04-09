package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.Points;

public interface PointsRepository extends ReactiveCrudRepository<Points, Long> {
    Flux<Points> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(amount),0) FROM points WHERE user_id=:userId")
    Mono<Long> sumByUserId(Long userId);
}
