package uz.sevenEdu.teacherBot.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class EskizSmsService {

    private final WebClient webClient;
    private final String email;
    private final String password;
    private String token;

    public EskizSmsService(
            @Value("${app.eskiz.email:test@eskiz.uz}") String email,
            @Value("${app.eskiz.password:test}") String password) {
        this.email = email;
        this.password = password;
        this.webClient = WebClient.builder()
                .baseUrl("https://notify.eskiz.uz/api")
                .build();
    }

    public Mono<Void> sendSms(String phone, String message) {
        return getToken()
                .flatMap(authToken -> webClient.post()
                        .uri("/message/sms/send")
                        .header("Authorization", "Bearer " + authToken)
                        .bodyValue(Map.of(
                                "mobile_phone", normalizePhone(phone),
                                "message", message,
                                "from", "4546"
                        ))
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnNext(resp -> log.info("Eskiz SMS response: {}", resp))
                        .then());
    }

    private Mono<String> getToken() {
        if (token != null) return Mono.just(token);
        return webClient.post()
                .uri("/auth/login")
                .bodyValue(Map.of("email", email, "password", password))
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) resp.get("data");
                    token = (String) data.get("token");
                    return token;
                })
                .onErrorResume(e -> {
                    log.error("Eskiz auth failed: {}", e.getMessage());
                    return Mono.just("");
                });
    }

    private String normalizePhone(String phone) {
        // Remove everything except digits
        String digits = phone.replaceAll("[^\\d]", "");
        // Ensure starts with 998
        if (!digits.startsWith("998") && digits.length() == 9) {
            digits = "998" + digits;
        }
        return digits;
    }
}
