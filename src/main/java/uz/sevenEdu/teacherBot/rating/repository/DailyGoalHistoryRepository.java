package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.DailyGoalHistory;

public interface DailyGoalHistoryRepository
        extends ReactiveCrudRepository<DailyGoalHistory, Long> {

    @Query("SELECT * FROM daily_goal_history WHERE user_id = :userId " +
           "ORDER BY history_date DESC LIMIT 60")
    Flux<DailyGoalHistory> findRecentByUserId(Long userId);

    /** Kun yakunida (yoki qayta) bugungi natijani saqlaydi. */
    @Modifying
    @Query("INSERT INTO daily_goal_history(user_id, history_date, minutes_done, words_done, minutes_goal, words_goal, percent) " +
           "VALUES (:userId, CURRENT_DATE, :minutesDone, :wordsDone, :minutesGoal, :wordsGoal, :percent) " +
           "ON CONFLICT (user_id, history_date) DO UPDATE SET " +
           "minutes_done = :minutesDone, words_done = :wordsDone, " +
           "minutes_goal = :minutesGoal, words_goal = :wordsGoal, percent = :percent")
    Mono<Integer> upsertToday(Long userId, int minutesDone, int wordsDone,
                              int minutesGoal, int wordsGoal, double percent);

    /** Cron (23:59) — BARCHA real o'quvchilarning bugungi natijasini BITTA set-based
     *  so'rov bilan saqlaydi (per-user N+1 o'rniga). minutes = daily_activity sekund/60,
     *  words = bugun yakunlangan darslardagi so'zlar, goal = settings (yo'q bo'lsa 30/20). */
    @Modifying
    @Query("""
        INSERT INTO daily_goal_history (user_id, history_date, minutes_done, words_done, minutes_goal, words_goal, percent)
        SELECT u.id, CURRENT_DATE,
               COALESCE(da.seconds_spent,0)/60,
               COALESCE(vw.words,0),
               COALESCE(gs.minutes_goal,30),
               COALESCE(gs.words_goal,20),
               ROUND((
                   LEAST(1.0, (COALESCE(da.seconds_spent,0)/60.0) / COALESCE(gs.minutes_goal,30))
                 + LEAST(1.0, COALESCE(vw.words,0)::numeric    / COALESCE(gs.words_goal,20))
               ) / 2.0 * 100)
        FROM users u
        LEFT JOIN daily_activity da ON da.user_id = u.id AND da.activity_date = CURRENT_DATE
        LEFT JOIN user_goal_settings gs ON gs.user_id = u.id
        LEFT JOIN (
            SELECT ul.user_id AS user_id, COUNT(v.id) AS words
            FROM user_lessons ul JOIN vocabulary v ON v.lesson_id = ul.lesson_id
            WHERE ul.is_completed = true AND ul.completed_at >= CURRENT_DATE
            GROUP BY ul.user_id
        ) vw ON vw.user_id = u.id
        WHERE u.role = 'STUDENT' AND (u.is_guest IS NULL OR u.is_guest = false)
        ON CONFLICT (user_id, history_date) DO UPDATE SET
            minutes_done = EXCLUDED.minutes_done,
            words_done   = EXCLUDED.words_done,
            minutes_goal = EXCLUDED.minutes_goal,
            words_goal   = EXCLUDED.words_goal,
            percent      = EXCLUDED.percent
        """)
    Mono<Integer> snapshotAllToday();
}
