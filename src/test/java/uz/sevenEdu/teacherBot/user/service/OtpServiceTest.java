package uz.sevenEdu.teacherBot.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceTest {

    private ReactiveStringRedisTemplate redis;
    private ReactiveValueOperations<String, String> values;
    private OtpService service;

    @BeforeEach
    void setUp() {
        redis = mock(ReactiveStringRedisTemplate.class);
        values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new OtpService(redis, mock(JavaMailSender.class), mock(UserRepository.class));
    }

    @Test
    void masterCodeIsNotAcceptedWithoutStoredOtp() {
        when(values.get("otp:user@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(service.verifyOtp("user@example.com", "55555"))
                .expectError(BadRequestException.class)
                .verify();

        verify(redis, never()).delete("otp:user@example.com");
    }

    @Test
    void correctStoredOtpIsConsumed() {
        when(values.get("otp:user@example.com")).thenReturn(Mono.just("12345"));
        when(redis.delete("otp:user@example.com")).thenReturn(Mono.just(1L));

        StepVerifier.create(service.verifyOtp("user@example.com", "12345"))
                .verifyComplete();

        verify(redis).delete("otp:user@example.com");
    }
}
