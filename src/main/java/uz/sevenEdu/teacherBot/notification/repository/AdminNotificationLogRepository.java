package uz.sevenEdu.teacherBot.notification.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.notification.entity.AdminNotificationLog;

public interface AdminNotificationLogRepository extends ReactiveCrudRepository<AdminNotificationLog, Long> {

    /** Oxirgi yuborilganlar — yangidan eskiga. */
    @Query("SELECT * FROM admin_notification_log ORDER BY created_at DESC, id DESC LIMIT :limit")
    Flux<AdminNotificationLog> findRecent(int limit);
}
