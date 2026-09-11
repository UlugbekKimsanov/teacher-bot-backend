package uz.sevenEdu.teacherBot.user.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.ServiceUnavailableException;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void smtpAuthenticationFailureBecomesServiceUnavailable() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        UserRepository users = mock(UserRepository.class);
        service = new OtpService(redis, mailSender, users);
        MimeMessage message = new MimeMessage((Session) null);

        when(users.existsByEmail("new-user@example.com")).thenReturn(Mono.just(false));
        when(values.set(
                eq("otp:new-user@example.com"),
                org.mockito.ArgumentMatchers.matches("\\d{5}"),
                eq(Duration.ofMinutes(5))))
                .thenReturn(Mono.just(true));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailAuthenticationException("bad credentials"))
                .when(mailSender).send(message);

        StepVerifier.create(service.sendOtp("new-user@example.com", false))
                .expectError(ServiceUnavailableException.class)
                .verify();
    }
}
