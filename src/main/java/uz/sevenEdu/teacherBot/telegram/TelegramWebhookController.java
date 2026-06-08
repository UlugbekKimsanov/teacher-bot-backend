package uz.sevenEdu.teacherBot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Telegram Bot webhook endpoint.
 * Telegram serverdan kelgan update larni qabul qiladi.
 * URL: POST /api/v1/telegram/webhook
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramBotService telegramBotService;

    /**
     * Telegram webhook — har bir xabar shu yerga keladi.
     * Auth kerak emas (Telegram server chaqiradi).
     */
    @PostMapping("/webhook")
    public Mono<Map<String, String>> handleUpdate(@RequestBody Map<String, Object> update) {
        try {
            Map<String, Object> message = (Map<String, Object>) update.get("message");
            if (message == null) {
                return Mono.just(Map.of("status", "ok"));
            }

            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
            if (chat == null) {
                return Mono.just(Map.of("status", "ok"));
            }

            Long chatId = ((Number) chat.get("id")).longValue();
            String text = (String) message.get("text");

            if (text != null && text.startsWith("/start")) {
                return telegramBotService.handleStartCommand(chatId, text)
                        .flatMap(reply -> telegramBotService.sendMessage(chatId, reply))
                        .thenReturn(Map.of("status", "ok"));
            }

            // Boshqa xabarlar uchun — yordam matni
            return telegramBotService.sendMessage(chatId,
                    "OAZIS Bot\nSotuv xabarlari avtomatik yuboriladi.\n"
                    + "Ilovadan \"Telegram ulash\" orqali ulaning.")
                    .thenReturn(Map.of("status", "ok"));

        } catch (Exception e) {
            log.warn("Telegram webhook xatosi: {}", e.getMessage());
            return Mono.just(Map.of("status", "ok"));
        }
    }
}
