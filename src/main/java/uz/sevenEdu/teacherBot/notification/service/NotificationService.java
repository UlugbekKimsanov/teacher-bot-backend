package uz.sevenEdu.teacherBot.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.notification.entity.Notification;
import uz.sevenEdu.teacherBot.notification.repository.NotificationRepository;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

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
        // 1) Ilova ichidagi xabarni saqlash  2) qurilmaga push yuborish (token bo'lsa)
        return notificationRepository.save(n)
                .flatMap(saved -> pushToUser(userId, title, body, type).thenReturn(saved));
    }

    /** Foydalanuvchining FCM tokeni bo'lsa, push yuboradi (fire-and-forget, xato bo'lsa e'tiborsiz). */
    private Mono<Void> pushToUser(Long userId, String title, String body, String type) {
        if (!fcmService.isEnabled()) return Mono.empty();
        return userRepository.findById(userId)
                .flatMap(u -> {
                    String token = u.getFcmToken();
                    if (token == null || token.isBlank()) return Mono.empty();
                    Map<String, String> data = new java.util.HashMap<>();
                    if (type != null) data.put("type", type);
                    return fcmService.send(token, title, body, data);
                })
                .onErrorResume(e -> Mono.empty());
    }

    /** Bir nechta foydalanuvchiga bir xil xabar yuborish. Yuborilgan soni qaytadi. */
    public Mono<Long> sendBulk(java.util.Collection<Long> userIds, String title, String body, String type) {
        return Flux.fromIterable(new java.util.LinkedHashSet<>(userIds))
                .flatMap(uid -> send(uid, title, body, type, null))
                .count();
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
