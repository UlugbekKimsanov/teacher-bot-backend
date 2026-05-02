package uz.sevenEdu.teacherBot.activity.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.activity.entity.Activity;

import java.time.LocalDateTime;

public interface ActivityRepository extends ReactiveCrudRepository<Activity, Long> {

    @Query("SELECT activity_type, SUM(value) as value " +
           "FROM activities " +
           "WHERE student_id = :studentId AND created_at >= :from " +
           "GROUP BY activity_type")
    Flux<ActivitySummaryRow> findSummaryByStudentIdAndPeriod(Long studentId, LocalDateTime from);

    interface ActivitySummaryRow {
        String getActivityType();
        Long getValue();
    }
}
