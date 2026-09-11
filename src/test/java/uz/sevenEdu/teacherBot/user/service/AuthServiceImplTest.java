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
import uz.sevenEdu.teacherBot.user.dto.GoogleAuthRequest;
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
    private GoogleIdentityVerifier googleIdentityVerifier;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        otpService = mock(OtpService.class);
        smsService = mock(EskizSmsService.class);
        redis = mock(ReactiveStringRedisTemplate.class);
        googleIdentityVerifier = mock(GoogleIdentityVerifier.class);
        values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new AuthServiceImpl(
                users, passwordEncoder, jwtUtil, otpService, smsService, redis,
                googleIdentityVerifier);
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

    @Test
    void googleAuthUsesVerifiedTokenIdentityAndCreatesLinkedUser() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("verified-id-token");
        GoogleIdentityVerifier.GoogleIdentity identity =
                new GoogleIdentityVerifier.GoogleIdentity(
                        "google-subject", "user@example.com", "Ali", "Valiyev");

        when(googleIdentityVerifier.verify("verified-id-token"))
                .thenReturn(Mono.just(identity));
        when(users.findByGoogleSubject("google-subject")).thenReturn(Mono.empty());
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Mono.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("random-password-hash");
        when(users.save(any(BaseUser.class))).thenAnswer(invocation -> {
            BaseUser user = invocation.getArgument(0);
            user.setId(77L);
            return Mono.just(user);
        });
        when(jwtUtil.generateToken(77L, null, UserRole.STUDENT, true))
                .thenReturn("jwt");

        StepVerifier.create(service.googleAuth(request))
                .assertNext(response -> {
                    assertThat(response.getEmail()).isEqualTo("user@example.com");
                    assertThat(response.getToken()).isEqualTo("jwt");
                })
                .verifyComplete();

        ArgumentCaptor<BaseUser> saved = ArgumentCaptor.forClass(BaseUser.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getGoogleSubject()).isEqualTo("google-subject");
        assertThat(saved.getValue().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void googleAuthLinksExistingEmailToVerifiedSubject() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("verified-id-token");
        GoogleIdentityVerifier.GoogleIdentity identity =
                new GoogleIdentityVerifier.GoogleIdentity(
                        "google-subject", "USER@example.com", "Ali", "Valiyev");
        BaseUser existing = BaseUser.builder()
                .id(9L)
                .email("user@example.com")
                .phone("+998901234567")
                .firstName("Existing")
                .lastName("User")
                .role(UserRole.STUDENT)
                .build();

        when(googleIdentityVerifier.verify("verified-id-token"))
                .thenReturn(Mono.just(identity));
        when(users.findByGoogleSubject("google-subject")).thenReturn(Mono.empty());
        when(users.findByEmailIgnoreCase("USER@example.com")).thenReturn(Mono.just(existing));
        when(users.save(existing)).thenReturn(Mono.just(existing));
        when(jwtUtil.generateToken(9L, "+998901234567", UserRole.STUDENT, true))
                .thenReturn("jwt");

        StepVerifier.create(service.googleAuth(request))
                .assertNext(response -> assertThat(response.getToken()).isEqualTo("jwt"))
                .verifyComplete();

        assertThat(existing.getGoogleSubject()).isEqualTo("google-subject");
        assertThat(existing.getFirstName()).isEqualTo("Existing");
        verify(users).save(existing);
    }
}
