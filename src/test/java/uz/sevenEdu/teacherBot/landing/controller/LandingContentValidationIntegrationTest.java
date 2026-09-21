package uz.sevenEdu.teacherBot.landing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.GlobalExceptionHandler;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.landing.dto.LandingContentDto;
import uz.sevenEdu.teacherBot.landing.service.LandingContentNormalizer;

class LandingContentValidationIntegrationTest {

    private ObjectMapper objectMapper;
    private ObjectNode validBody;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        LandingContentNormalizer normalizer = new LandingContentNormalizer(
                objectMapper,
                Validation.buildDefaultValidatorFactory().getValidator()
        );
        validBody = objectMapper.valueToTree(normalizer.defaultContent());
        client = WebTestClient.bindToController(new ValidationController())
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidSchemaVersionReturns400() {
        validBody.put("schemaVersion", 1);

        put(validBody)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").value(message ->
                        org.assertj.core.api.Assertions.assertThat(message.toString()).contains("schemaVersion"));
    }

    @Test
    void javascriptUrlReturns400() {
        ((ObjectNode) validBody.withArray("courses").get(0)).put("buyUrl", "javascript:alert(1)");

        put(validBody)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").value(message ->
                        org.assertj.core.api.Assertions.assertThat(message.toString()).contains("buyUrl"));
    }

    @Test
    void dataImageUrlReturns400() {
        ((ObjectNode) validBody.withArray("goals").get(0)).put("image", "data:image/svg+xml,<svg/>");

        put(validBody)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void canonicalV2Returns200() {
        put(validBody)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.schemaVersion").isEqualTo(2);
    }

    private WebTestClient.ResponseSpec put(ObjectNode body) {
        return client.put()
                .uri("/content")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    @RestController
    static class ValidationController {
        @PutMapping("/content")
        Mono<ApiResponse<LandingContentDto>> update(@Valid @RequestBody LandingContentDto content) {
            return Mono.just(ApiResponse.ok(content));
        }
    }
}
