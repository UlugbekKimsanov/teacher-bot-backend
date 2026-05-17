package uz.sevenEdu.teacherBot.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.notification.entity.Notification;
import uz.sevenEdu.teacherBot.notification.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public Mono<ApiResponse<List<Notification>>> getAll(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return notificationService.getUserNotifications(userId)
                .collectList()
                .map(ApiResponse::ok);
    }

    @GetMapping("/unread-count")
    public Mono<ApiResponse<Long>> getUnreadCount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return notificationService.getUnreadCount(userId)
                .map(ApiResponse::ok);
    }

    @PatchMapping("/{id}/read")
    public Mono<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return notificationService.markAsRead(id, userId)
                .then(Mono.just(ApiResponse.ok("OK", null)));
    }

    @PatchMapping("/read-all")
    public Mono<ApiResponse<Void>> markAllAsRead(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return notificationService.markAllAsRead(userId)
                .then(Mono.just(ApiResponse.ok("OK", null)));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> deleteNotification(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return notificationService.deleteNotification(id, userId)
                .then(Mono.just(ApiResponse.ok("OK", null)));
    }

    @PostMapping("/fcm-token")
    public Mono<ApiResponse<Void>> saveFcmToken(@RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String token = body.get("token");
        return notificationService.saveFcmToken(userId, token)
                .then(Mono.just(ApiResponse.ok("OK", null)));
    }
}
