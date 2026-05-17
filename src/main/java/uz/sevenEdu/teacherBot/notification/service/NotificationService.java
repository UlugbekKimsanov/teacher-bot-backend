package uz.sevenEdu.teacherBot.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.notification.entity.Notification;
import uz.sevenEdu.teacherBot.notification.repository.NotificationRepository;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Flux<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Mono<Long> getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public Mono<Void> markAsRead(Long notificationId, Long userId) {
        return notificationRepository.markAsRead(notificationId, userId).then();
    }

    public Mono<Void> markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId).then();
    }

    public Mono<Notification> send(Long userId, String title, String body, String type, Long refId) {
        Notification n = Notification.builder()
                .userId(userId)
                .title(title)
                .body(body)
                .type(type)
                .refId(refId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(n);
    }

    public Mono<Void> deleteNotification(Long notificationId, Long userId) {
        return notificationRepository.deleteByIdAndUserId(notificationId, userId).then();
    }

    public Mono<Void> saveFcmToken(Long userId, String fcmToken) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setFcmToken(fcmToken);
                    return userRepository.save(user);
                })
                .then();
    }
}
