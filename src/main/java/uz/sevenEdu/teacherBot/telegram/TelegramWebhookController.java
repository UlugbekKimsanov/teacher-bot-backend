package uz.sevenEdu.teacherBot.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {
    private final TelegramBotService telegramBotService;

    @PostMapping("/webhook")
    public Mono<Map<String, String>> handleUpdate(@RequestBody JsonNode update) {
        return telegramBotService.handleUpdate(update)
                .onErrorResume(error -> {
                    log.warn("Telegram webhook xatosi: {}", error.getMessage());
                    return Mono.empty();
                })
                .thenReturn(Map.of("status", "ok"));
    }
}
