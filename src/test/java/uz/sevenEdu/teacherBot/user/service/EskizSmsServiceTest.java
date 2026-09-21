package uz.sevenEdu.teacherBot.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService.EskizRuntimeSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EskizSmsServiceTest {

    @Test
    void usesMultipartAndRetriesSendOnceWithFreshTokenAfter401() {
        IntegrationSettingsService settingsService = mock(IntegrationSettingsService.class);
        when(settingsService.resolveEskiz()).thenReturn(Mono.just(
                new EskizRuntimeSettings("sms@example.com", "secret", "4546")));

        List<ClientRequest> requests = new ArrayList<>();
        AtomicInteger call = new AtomicInteger();
        ExchangeFunction exchange = request -> {
            requests.add(request);
            return Mono.just(switch (call.getAndIncrement()) {
                case 0 -> json(HttpStatus.OK, "{\"data\":{\"token\":\"old-token\"}}");
                case 1 -> json(HttpStatus.UNAUTHORIZED, "{\"message\":\"expired\"}");
                case 2 -> json(HttpStatus.OK, "{\"data\":{\"token\":\"new-token\"}}");
                default -> json(HttpStatus.OK, "{\"status\":\"waiting\"}");
            });
        };
        EskizSmsService service = new EskizSmsService(
                settingsService, WebClient.builder().exchangeFunction(exchange).build());

        StepVerifier.create(service.sendSms("+998 (90) 123-45-67", "Kod: 12345"))
                .verifyComplete();

        assertThat(requests).hasSize(4);
        assertThat(requests).extracting(request -> request.url().getPath())
                .containsExactly("/auth/login", "/message/sms/send", "/auth/login", "/message/sms/send");
        assertThat(requests).allSatisfy(request ->
                assertThat(request.headers().getContentType()).isEqualTo(MediaType.MULTIPART_FORM_DATA));
        assertThat(requests.get(1).headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer old-token");
        assertThat(requests.get(3).headers().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer new-token");
    }

    @Test
    void authenticationFailureIsPropagatedAndSmsIsNotAttempted() {
        IntegrationSettingsService settingsService = mock(IntegrationSettingsService.class);
        when(settingsService.resolveEskiz()).thenReturn(Mono.just(
                new EskizRuntimeSettings("bad@example.com", "wrong", "4546")));
        List<ClientRequest> requests = new ArrayList<>();
        ExchangeFunction exchange = request -> {
            requests.add(request);
            return Mono.just(json(HttpStatus.UNAUTHORIZED, "{\"message\":\"invalid credentials\"}"));
        };
        EskizSmsService service = new EskizSmsService(
                settingsService, WebClient.builder().exchangeFunction(exchange).build());

        StepVerifier.create(service.sendSms("+998901234567", "Kod: 12345"))
                .expectError(WebClientResponseException.Unauthorized.class)
                .verify();

        assertThat(requests).singleElement().satisfies(request ->
                assertThat(request.url().getPath()).isEqualTo("/auth/login"));
    }

    private ClientResponse json(HttpStatus status, String body) {
        return ClientResponse.create(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }
}
