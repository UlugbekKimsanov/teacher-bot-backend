package uz.sevenEdu.teacherBot.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotRunner {
    private static final String TELEGRAM_API = "https://api.telegram.org";

    private final TelegramProperties properties;
    private final TelegramBotService botService;
    private final WebClient.Builder webClientBuilder;
    private final AtomicLong offset = new AtomicLong(0);
    private Disposable polling;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isPollingEnabled() || properties.getBotToken() == null
                || properties.getBotToken().isBlank()) {
            log.info("Telegram bot polling o'chiq: token berilmagan yoki polling o'chirilgan");
            return;
        }

        polling = deleteWebhook()
                .thenMany(Flux.defer(this::pollOnce).repeat())
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal -> log.warn("Telegram polling qayta ulanmoqda: {}",
                                signal.failure().getMessage())))
                .subscribe(null, error -> log.error("Telegram bot polling to'xtadi", error));
        log.info("Telegram bot avtomatik ishga tushdi (long polling)");
    }

    private Mono<Void> deleteWebhook() {
        return client().post()
                .uri("/bot{token}/deleteWebhook?drop_pending_updates=false", properties.getBotToken())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .then();
    }

    private Mono<Void> pollOnce() {
        return client().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/bot{token}/getUpdates")
                        .queryParam("offset", offset.get())
                        .queryParam("timeout", 25)
                        .queryParam("allowed_updates", "[\"message\"]")
                        .build(properties.getBotToken()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(35))
                .flatMapMany(response -> {
                    List<JsonNode> updates = new ArrayList<>();
                    response.path("result").forEach(updates::add);
                    return Flux.fromIterable(updates);
                })
                .concatMap(update -> {
                    offset.set(Math.max(offset.get(), update.path("update_id").asLong() + 1));
                    return botService.handleUpdate(update);
                })
                .then();
    }

    private WebClient client() {
        return webClientBuilder.clone().baseUrl(TELEGRAM_API).build();
    }

    @PreDestroy
    public void stop() {
        if (polling != null) polling.dispose();
    }
}
