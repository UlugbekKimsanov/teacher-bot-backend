package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.dto.LeaderboardRow;
import uz.sevenEdu.teacherBot.rating.entity.Points;

public interface PointsRepository extends ReactiveCrudRepository<Points, Long> {
    Flux<Points> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Oxirgi 100 ta ball yozuvi (ro'yxat cheksiz o'smasin). */
    Flux<Points> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(amount),0) FROM points WHERE user_id = :userId")
    Mono<Long> sumByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(amount),0) FROM points WHERE user_id = :userId AND created_at >= CURRENT_DATE")
    Mono<Long> sumTodayByUserId(Long userId);

    /** Leaderboard top-50 — bitta GROUP BY so'rov (N+1 o'rniga). */
    @Query("""
        SELECT u.id AS id, u.first_name AS first_name, u.last_name AS last_name,
               COALESCE(SUM(p.amount),0) AS total
        FROM users u LEFT JOIN points p ON p.user_id = u.id
        WHERE u.role = 'STUDENT' AND (u.is_guest IS NULL OR u.is_guest = false)
        GROUP BY u.id, u.first_name, u.last_name
        ORDER BY total DESC
        LIMIT 50
        """)
    Flux<LeaderboardRow> findTop50Leaderboard();

    /** Mendan yuqori (ko'proq ballik) o'quvchilar soni — rank hisoblash uchun. */
    @Query("""
        SELECT COUNT(*) FROM (
            SELECT COALESCE(SUM(p.amount),0) AS total
            FROM users u LEFT JOIN points p ON p.user_id = u.id
            WHERE u.role = 'STUDENT' AND (u.is_guest IS NULL OR u.is_guest = false)
            GROUP BY u.id
            HAVING COALESCE(SUM(p.amount),0) > :myTotal
        ) x
        """)
    Mono<Long> countRankedAbove(long myTotal);
}
