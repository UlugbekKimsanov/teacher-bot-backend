package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.DailyActivity;

public interface DailyActivityRepository
        extends ReactiveCrudRepository<DailyActivity, Long> {

    /** Bugungi sarflangan sekundga qo'shadi (yo'q bo'lsa yaratadi). */
    @Modifying
    @Query("INSERT INTO daily_activity(user_id, activity_date, seconds_spent) " +
           "VALUES (:userId, CURRENT_DATE, :seconds) " +
           "ON CONFLICT (user_id, activity_date) DO UPDATE SET " +
           "seconds_spent = daily_activity.seconds_spent + :seconds")
    Mono<Integer> addSecondsToday(Long userId, int seconds);

    @Query("SELECT COALESCE(seconds_spent, 0) FROM daily_activity " +
           "WHERE user_id = :userId AND activity_date = CURRENT_DATE")
    Mono<Integer> todaySeconds(Long userId);
}
