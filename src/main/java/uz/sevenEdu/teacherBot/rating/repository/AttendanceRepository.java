package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.Attendance;

import java.time.LocalDate;

public interface AttendanceRepository extends ReactiveCrudRepository<Attendance, Long> {

    @Query("SELECT COUNT(*) FROM attendance WHERE user_id = :userId AND course_id = :courseId AND attended_at >= CURRENT_DATE - INTERVAL '7 days'")
    Mono<Long> countWeekly(Long userId, Long courseId);

    @Query("SELECT COUNT(*) FROM attendance WHERE user_id = :userId AND course_id = :courseId AND attended_at >= CURRENT_DATE - INTERVAL '30 days'")
    Mono<Long> countMonthly(Long userId, Long courseId);

    @Query("SELECT COUNT(*) FROM attendance WHERE user_id = :userId AND course_id = :courseId AND attended_at >= CURRENT_DATE - INTERVAL '120 days'")
    Mono<Long> countQuarterly(Long userId, Long courseId);

    /** Distinct attended dates for a user across all courses, ordered DESC */
    @Query("SELECT DISTINCT attended_at FROM attendance WHERE user_id = :userId ORDER BY attended_at DESC")
    Flux<LocalDate> findDistinctDatesByUserId(Long userId);

    /** Record attendance for today (idempotent) */
    @Modifying
    @Query("INSERT INTO attendance (user_id, course_id, attended_at) VALUES (:userId, :courseId, CURRENT_DATE) ON CONFLICT (user_id, course_id, attended_at) DO NOTHING")
    Mono<Integer> recordToday(Long userId, Long courseId);

    /** Bugun davomat qilgan (faol) o'quvchilar soni (distinct) */
    @Query("SELECT COUNT(DISTINCT user_id) FROM attendance WHERE attended_at = CURRENT_DATE")
    Mono<Long> countActiveToday();
}
