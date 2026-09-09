package uz.sevenEdu.teacherBot.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService.EskizRuntimeSettings;
import uz.sevenEdu.teacherBot.user.util.PhoneNumberUtil;

import java.time.Duration;

@Slf4j
@Service
public class EskizSmsService {

    private final IntegrationSettingsService settingsService;
    private final WebClient webClient;
    private volatile CachedToken cachedToken;

    @Autowired
    public EskizSmsService(
            IntegrationSettingsService settingsService,
            @Value("${app.eskiz.base-url:https://notify.eskiz.uz/api}") String baseUrl) {
        this(settingsService, createWebClient(baseUrl));
    }

    EskizSmsService(IntegrationSettingsService settingsService, WebClient webClient) {
        this.settingsService = settingsService;
        this.webClient = webClient;
    }

    public Mono<Void> sendSms(String phone, String message) {
        String providerPhone = PhoneNumberUtil.digits(PhoneNumberUtil.normalizeUzbekPhone(phone));
        return settingsService.resolveEskiz().flatMap(settings -> validate(settings)
                .then(getToken(settings))
                .flatMap(token -> sendWithToken(token, providerPhone, message, settings.from())
                        .onErrorResume(WebClientResponseException.Unauthorized.class,
                                error -> retryOnce(token, settings, providerPhone, message))));
    }

    private Mono<Void> sendWithToken(String token, String phone, String message, String sender) {
        return webClient.post()
                .uri("/message/sms/send")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("mobile_phone", phone)
                        .with("message", message)
                        .with("from", sender))
                .retrieve()
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .doOnNext(response -> log.info("Eskiz SMS request accepted"))
                .then();
    }

    private Mono<Void> retryOnce(String rejectedToken, EskizRuntimeSettings settings,
                                 String phone, String message) {
        invalidateToken(rejectedToken);
        return authenticate(settings)
                .flatMap(token -> sendWithToken(token, phone, message, settings.from()));
    }

    private Mono<String> getToken(EskizRuntimeSettings settings) {
        CachedToken current = cachedToken;
        return current != null && current.matches(settings)
                ? Mono.just(current.value())
                : authenticate(settings);
    }

    private Mono<String> authenticate(EskizRuntimeSettings settings) {
        return webClient.post()
                .uri("/auth/login")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData("email", settings.email())
                        .with("password", settings.password()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> response.path("data").path("token").asText(""))
                .filter(value -> !value.isBlank())
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Eskiz autentifikatsiya javobida token yo'q")))
                .doOnNext(value -> cachedToken = new CachedToken(
                        settings.email(), settings.password(), value))
                .doOnError(error -> log.error("Eskiz authentication failed: {}", error.getMessage()));
    }

    private Mono<Void> validate(EskizRuntimeSettings settings) {
        if (settings == null || isBlank(settings.email())
                || isBlank(settings.password()) || isBlank(settings.from())) {
            return Mono.error(new IllegalStateException("Eskiz SMS sozlamalari kiritilmagan"));
        }
        return Mono.empty();
    }

    private void invalidateToken(String rejectedToken) {
        CachedToken current = cachedToken;
        if (current != null && current.value().equals(rejectedToken)) cachedToken = null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static WebClient createWebClient(String baseUrl) {
        HttpClient client = HttpClient.create().responseTimeout(Duration.ofSeconds(10));
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(client))
                .build();
    }

    private record CachedToken(String email, String password, String value) {
        private boolean matches(EskizRuntimeSettings settings) {
            return email.equals(settings.email()) && password.equals(settings.password());
        }
    }
}
