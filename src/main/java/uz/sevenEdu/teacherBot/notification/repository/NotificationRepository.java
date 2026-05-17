package uz.sevenEdu.teacherBot.notification.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.notification.entity.Notification;

public interface NotificationRepository extends ReactiveCrudRepository<Notification, Long> {
    Flux<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND is_read = false ORDER BY created_at DESC")
    Flux<Notification> findUnreadByUserId(Long userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = false")
    Mono<Long> countUnreadByUserId(Long userId);

    @Modifying
    @Query("UPDATE notifications SET is_read = true WHERE id = :id AND user_id = :userId")
    Mono<Integer> markAsRead(Long id, Long userId);

    @Modifying
    @Query("UPDATE notifications SET is_read = true WHERE user_id = :userId")
    Mono<Integer> markAllAsRead(Long userId);

    @Modifying
    @Query("DELETE FROM notifications WHERE id = :id AND user_id = :userId")
    Mono<Integer> deleteByIdAndUserId(Long id, Long userId);
}
