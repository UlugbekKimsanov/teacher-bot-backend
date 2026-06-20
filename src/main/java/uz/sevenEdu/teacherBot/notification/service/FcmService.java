package uz.sevenEdu.teacherBot.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * FCM (push) yuborish. Firebase sozlanmagan bo'lsa — xavfsiz no-op.
 * Bloklovchi Firebase chaqiruvlari boundedElastic'da bajariladi.
 */
@Slf4j
@Service
public class FcmService {

    private final FirebaseMessaging messaging; // null bo'lishi mumkin (push o'chiq)

    public FcmService(ObjectProvider<FirebaseMessaging> messagingProvider) {
        this.messaging = messagingProvider.getIfAvailable();
    }

    public boolean isEnabled() {
        return messaging != null;
    }

    /** Bitta qurilmaga (token) push yuborish. Token yo'q yoki push o'chiq bo'lsa — no-op. */
    public Mono<Void> send(String token, String title, String body, Map<String, String> data) {
        if (messaging == null || token == null || token.isBlank()) {
            return Mono.empty();
        }
        return Mono.fromCallable(() -> {
                    Message.Builder builder = Message.builder()
                            .setToken(token)
                            .setNotification(com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build());
                    if (data != null && !data.isEmpty()) {
                        builder.putAllData(data);
                    }
                    return messaging.send(builder.build());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("FCM push xatosi: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}
