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
import uz.sevenEdu.teacherBot.settings.event.IntegrationSettingsChangedEvent;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService.TelegramRuntimeSettings;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotRunner {
    private static final String TELEGRAM_API = "https://api.telegram.org";

    private final IntegrationSettingsService settingsService;
    private final TelegramBotService botService;
    private final WebClient.Builder webClientBuilder;
    private final AtomicLong offset = new AtomicLong(0);
    private Disposable polling;
    private long generation;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        restart();
    }

    @EventListener(IntegrationSettingsChangedEvent.class)
    public void onSettingsChanged() {
        restart();
    }

    /** Admin sozlamani saqlaganda amaldagi pollerni xavfsiz qayta ishga tushiradi. */
    public synchronized void restart() {
        stopPolling();
        offset.set(0);
        long requestedGeneration = ++generation;
        settingsService.resolveTelegram().subscribe(
                settings -> startResolved(settings, requestedGeneration),
                error -> log.error("Telegram sozlamalarini yuklab bo'lmadi: {}",
                        error.getClass().getSimpleName())
        );
    }

    private synchronized void startResolved(TelegramRuntimeSettings settings, long requestedGeneration) {
        if (requestedGeneration != generation) return;
        if (!settings.pollingEnabled() || settings.botToken() == null || settings.botToken().isBlank()) {
            log.info("Telegram bot polling o'chiq: token berilmagan yoki polling o'chirilgan");
            return;
        }

        String token = settings.botToken();

        polling = deleteWebhook(token)
                .thenMany(Flux.defer(() -> pollOnce(token)).repeat())
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal -> log.warn("Telegram polling qayta ulanmoqda ({})",
                                signal.failure().getClass().getSimpleName())))
                .subscribe(null, error -> log.error("Telegram bot polling to'xtadi ({})",
                        error.getClass().getSimpleName()));
        log.info("Telegram bot avtomatik ishga tushdi (long polling)");
    }

    private Mono<Void> deleteWebhook(String token) {
        return client().post()
                .uri("/bot{token}/deleteWebhook?drop_pending_updates=false", token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .then();
    }

    private Mono<Void> pollOnce(String token) {
        return client().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/bot{token}/getUpdates")
                        .queryParam("offset", offset.get())
                        .queryParam("timeout", 25)
                        .queryParam("allowed_updates", "[\"message\"]")
                        .build(token))
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
    public synchronized void stop() {
        generation++;
        stopPolling();
    }

    private void stopPolling() {
        if (polling != null) {
            polling.dispose();
            polling = null;
        }
    }
}
