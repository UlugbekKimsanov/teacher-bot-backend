package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.UserAchievement;

public interface UserAchievementRepository extends ReactiveCrudRepository<UserAchievement, Long> {
    Flux<UserAchievement> findByUserId(Long userId);
    Mono<Boolean> existsByUserIdAndAchievementId(Long userId, Long achievementId);

    @Query("SELECT COUNT(*) FROM user_achievements WHERE user_id = :userId")
    Mono<Long> countByUserId(Long userId);
}
