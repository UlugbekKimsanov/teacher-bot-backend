package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.UserGoalSettings;

public interface UserGoalSettingsRepository
        extends ReactiveCrudRepository<UserGoalSettings, Long> {

    @Modifying
    @Query("INSERT INTO user_goal_settings(user_id, minutes_goal, words_goal, updated_at) " +
           "VALUES (:userId, :minutesGoal, :wordsGoal, now()) " +
           "ON CONFLICT (user_id) DO UPDATE SET " +
           "minutes_goal = :minutesGoal, words_goal = :wordsGoal, updated_at = now()")
    Mono<Integer> upsert(Long userId, int minutesGoal, int wordsGoal);
}
