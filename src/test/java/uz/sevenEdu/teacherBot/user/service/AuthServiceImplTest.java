package uz.sevenEdu.teacherBot.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.user.dto.LoginRequest;
import uz.sevenEdu.teacherBot.user.dto.RegisterRequest;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;
import uz.sevenEdu.teacherBot.user.security.JwtUtil;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private UserRepository users;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private OtpService otpService;
    private EskizSmsService smsService;
    private ReactiveStringRedisTemplate redis;
    private ReactiveValueOperations<String, String> values;
    private GoogleIdentityVerifier googleVerifier;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        otpService = mock(OtpService.class);
        smsService = mock(EskizSmsService.class);
        redis = mock(ReactiveStringRedisTemplate.class);
        values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        googleVerifier = mock(GoogleIdentityVerifier.class);
        service = new AuthServiceImpl(users, passwordEncoder, jwtUtil, otpService, smsService, redis, googleVerifier);
    }

    @Test
    void registerStoresCanonicalPhoneAndChecksLegacyCompatibleKey() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Ali");
        request.setLastName("Valiyev");
        request.setEmail("ali@example.com");
        request.setPhone("+998 (90) 123 45 67");
        request.setPassword("secret");
        request.setOtpCode("12345");

        when(otpService.verifyOtp("ali@example.com", "12345")).thenReturn(Mono.empty());
        when(users.existsByPhoneDigits("998901234567")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(users.save(any(BaseUser.class))).thenAnswer(invocation -> {
            BaseUser user = invocation.getArgument(0);
            user.setId(42L);
            return Mono.just(user);
        });
        when(jwtUtil.generateToken(42L, "+998901234567", UserRole.STUDENT, true))
                .thenReturn("jwt");

        StepVerifier.create(service.register(request))
                .assertNext(response -> assertThat(response.getPhone()).isEqualTo("+998901234567"))
                .verifyComplete();

        ArgumentCaptor<BaseUser> saved = ArgumentCaptor.forClass(BaseUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPhone()).isEqualTo("+998901234567");
    }

    @Test
    void sendPhoneOtpUsesCanonicalRedisSmsAndLegacyCompatibleLookup() {
        when(users.existsByPhoneDigits("998901234567")).thenReturn(Mono.just(true));
        when(values.set(
                eq("phone_otp:+998901234567"),
                argThat(code -> code != null && code.matches("\\d{5}")),
                eq(Duration.ofMinutes(5))))
                .thenReturn(Mono.just(true));
        when(smsService.sendSms(
                eq("+998901234567"),
                argThat(message -> message != null && message.matches("OAZIS tasdiqlash kodi: \\d{5}"))))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.sendPhoneOtp("+998 (90) 123-45-67", true))
                .verifyComplete();

        verify(users).existsByPhoneDigits("998901234567");
    }

    @Test
    void phoneLoginDoesNotAcceptOldMasterCodeWithoutStoredOtp() {
        LoginRequest request = new LoginRequest();
        request.setEmail("+998 (90) 123 45 67");
        request.setPassword("55555");
        request.setMobile(true);
        when(values.get("phone_otp:+998901234567")).thenReturn(Mono.empty());

        StepVerifier.create(service.phoneLogin(request))
                .expectError(BadRequestException.class)
                .verify();

        verify(users, never()).findByPhoneDigits("998901234567");
    }
}
