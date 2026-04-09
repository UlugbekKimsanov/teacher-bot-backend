package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.Attendance;

public interface AttendanceRepository extends ReactiveCrudRepository<Attendance, Long> {

    @Query("SELECT COUNT(*) FROM attendance WHERE user_id=:userId AND course_id=:courseId AND attended_at >= CURRENT_DATE - INTERVAL '7 days'")
    Mono<Long> countWeekly(Long userId, Long courseId);

    @Query("SELECT COUNT(*) FROM attendance WHERE user_id=:userId AND course_id=:courseId AND attended_at >= CURRENT_DATE - INTERVAL '30 days'")
    Mono<Long> countMonthly(Long userId, Long courseId);

    @Query("SELECT COUNT(*) FROM attendance WHERE user_id=:userId AND course_id=:courseId AND attended_at >= CURRENT_DATE - INTERVAL '120 days'")
    Mono<Long> countQuarterly(Long userId, Long courseId);
}
