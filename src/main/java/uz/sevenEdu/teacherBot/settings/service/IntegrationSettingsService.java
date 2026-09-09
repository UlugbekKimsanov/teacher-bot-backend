package uz.sevenEdu.teacherBot.settings.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.settings.dto.IntegrationSettingsResponse;
import uz.sevenEdu.teacherBot.settings.dto.UpdateIntegrationSettingsRequest;
import uz.sevenEdu.teacherBot.settings.entity.IntegrationSettings;
import uz.sevenEdu.teacherBot.settings.event.IntegrationSettingsChangedEvent;
import uz.sevenEdu.teacherBot.settings.repository.IntegrationSettingsRepository;
import uz.sevenEdu.teacherBot.telegram.TelegramProperties;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class IntegrationSettingsService {

    private static final long SETTINGS_ID = 1L;
    private static final String MASK = "********";
    private static final Pattern TELEGRAM_USERNAME = Pattern.compile("[A-Za-z0-9_]{5,64}");

    private final IntegrationSettingsRepository repository;
    private final TelegramProperties telegramFallback;
    private final ApplicationEventPublisher eventPublisher;
    private final String eskizEmailFallback;
    private final String eskizPasswordFallback;
    private final String eskizFromFallback;

    public IntegrationSettingsService(
            IntegrationSettingsRepository repository,
            TelegramProperties telegramFallback,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.eskiz.email:}") String eskizEmailFallback,
            @Value("${app.eskiz.password:}") String eskizPasswordFallback,
            @Value("${app.eskiz.from:4546}") String eskizFromFallback
    ) {
        this.repository = repository;
        this.telegramFallback = telegramFallback;
        this.eventPublisher = eventPublisher;
        this.eskizEmailFallback = clean(eskizEmailFallback);
        this.eskizPasswordFallback = cleanSecret(eskizPasswordFallback);
        this.eskizFromFallback = clean(eskizFromFallback);
    }

    public Mono<IntegrationSettingsResponse> getAdminSettings() {
        return loadStored().map(this::toResponse);
    }

    public Mono<IntegrationSettingsResponse> update(
            UpdateIntegrationSettingsRequest request,
            Long updatedBy
    ) {
        return loadStored()
                .flatMap(current -> {
                    String username = preserveWhenBlank(
                            normalizeUsername(request.getTelegramBotUsername()),
                            current.getTelegramBotUsername()
                    );
                    validateUsername(username);

                    String token = preserveSecret(
                            request.getTelegramBotToken(),
                            current.getTelegramBotToken()
                    );
                    String eskizEmail = preserveWhenBlank(
                            clean(request.getEskizEmail()),
                            current.getEskizEmail()
                    );
                    String eskizPassword = preserveSecret(
                            request.getEskizPassword(),
                            current.getEskizPassword()
                    );
                    String eskizFrom = preserveWhenBlank(
                            clean(request.getEskizFrom()),
                            current.getEskizFrom()
                    );
                    Boolean pollingEnabled = request.getTelegramPollingEnabled() != null
                            ? request.getTelegramPollingEnabled()
                            : current.getTelegramPollingEnabled();
                    LocalDateTime updatedAt = LocalDateTime.now();

                    return repository.upsert(
                            token,
                            username,
                            pollingEnabled,
                            eskizEmail,
                            eskizPassword,
                            eskizFrom,
                            updatedAt,
                            updatedBy
                    );
                })
                .doOnNext(ignored -> eventPublisher.publishEvent(new IntegrationSettingsChangedEvent()))
                .map(this::toResponse);
    }

    /** Runtime Telegram qiymatlari: DB override, bo'lmasa environment/application fallback. */
    public Mono<TelegramRuntimeSettings> resolveTelegram() {
        return loadStored().map(settings -> new TelegramRuntimeSettings(
                firstNonBlank(settings.getTelegramBotToken(), cleanSecret(telegramFallback.getBotToken())),
                normalizeUsername(firstNonBlank(
                        settings.getTelegramBotUsername(),
                        telegramFallback.getBotUsername()
                )),
                settings.getTelegramPollingEnabled() != null
                        ? settings.getTelegramPollingEnabled()
                        : telegramFallback.isPollingEnabled()
        ));
    }

    /** Runtime Eskiz qiymatlari: DB override, bo'lmasa environment/application fallback. */
    public Mono<EskizRuntimeSettings> resolveEskiz() {
        return loadStored().map(settings -> new EskizRuntimeSettings(
                firstNonBlank(settings.getEskizEmail(), eskizEmailFallback),
                firstNonBlank(settings.getEskizPassword(), eskizPasswordFallback),
                firstNonBlank(settings.getEskizFrom(), eskizFromFallback)
        ));
    }

    private Mono<IntegrationSettings> loadStored() {
        return repository.findById(SETTINGS_ID)
                .defaultIfEmpty(IntegrationSettings.builder().id(SETTINGS_ID).build());
    }

    private IntegrationSettingsResponse toResponse(IntegrationSettings settings) {
        String effectiveToken = firstNonBlank(
                settings.getTelegramBotToken(),
                cleanSecret(telegramFallback.getBotToken())
        );
        String effectiveEmail = firstNonBlank(settings.getEskizEmail(), eskizEmailFallback);
        String effectivePassword = firstNonBlank(settings.getEskizPassword(), eskizPasswordFallback);
        String effectiveUsername = normalizeUsername(firstNonBlank(
                settings.getTelegramBotUsername(),
                telegramFallback.getBotUsername()
        ));
        String effectiveFrom = firstNonBlank(settings.getEskizFrom(), eskizFromFallback);

        String tokenSource = hasText(settings.getTelegramBotToken())
                ? "database"
                : hasText(effectiveToken) ? "environment" : "none";
        String credentialsSource = hasText(settings.getEskizEmail()) || hasText(settings.getEskizPassword())
                ? "database"
                : hasText(effectiveEmail) || hasText(effectivePassword) ? "environment" : "none";

        return new IntegrationSettingsResponse(
                new IntegrationSettingsResponse.TelegramSettings(
                        valueOrEmpty(effectiveUsername),
                        settings.getTelegramPollingEnabled() != null
                                ? settings.getTelegramPollingEnabled()
                                : telegramFallback.isPollingEnabled(),
                        hasText(effectiveToken),
                        hasText(effectiveToken) ? MASK : "",
                        tokenSource
                ),
                new IntegrationSettingsResponse.EskizSettings(
                        hasText(effectiveEmail),
                        hasText(effectiveEmail) ? MASK : "",
                        hasText(effectivePassword),
                        hasText(effectivePassword) ? MASK : "",
                        valueOrEmpty(effectiveFrom),
                        credentialsSource
                ),
                settings.getUpdatedAt()
        );
    }

    private static String preserveSecret(String candidate, String current) {
        String cleaned = cleanSecret(candidate);
        return hasText(cleaned) ? cleaned : current;
    }

    private static String preserveWhenBlank(String candidate, String current) {
        return hasText(candidate) ? candidate : current;
    }

    private static String firstNonBlank(String primary, String fallback) {
        return hasText(primary) ? primary : fallback;
    }

    private static String normalizeUsername(String value) {
        String cleaned = clean(value);
        if (cleaned != null && cleaned.startsWith("@")) {
            cleaned = clean(cleaned.substring(1));
        }
        return cleaned;
    }

    private static void validateUsername(String username) {
        if (hasText(username) && !TELEGRAM_USERNAME.matcher(username).matches()) {
            throw new BadRequestException(
                    "Telegram username 5-64 ta lotin harfi, raqam yoki pastki chiziqdan iborat bo'lishi kerak"
            );
        }
    }

    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String cleanSecret(String value) {
        return clean(value);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public record TelegramRuntimeSettings(
            String botToken,
            String botUsername,
            boolean pollingEnabled
    ) {
    }

    public record EskizRuntimeSettings(
            String email,
            String password,
            String from
    ) {
    }
}
