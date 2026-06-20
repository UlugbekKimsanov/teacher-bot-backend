package uz.sevenEdu.teacherBot.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.dto.*;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;
import uz.sevenEdu.teacherBot.user.security.JwtUtil;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final EskizSmsService eskizSmsService;
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String PHONE_OTP_PREFIX = "phone_otp:";

    @Override
    public Mono<Void> sendOtp(String email, boolean isLogin) {
        return otpService.sendOtp(email, isLogin);
    }

    @Override
    public Mono<AuthResponse> register(RegisterRequest request) {
        return otpService.verifyOtp(request.getEmail(), request.getOtpCode())
                .then(userRepository.existsByPhone(request.getPhone()))
                .flatMap(exists -> {
                    if (exists) return Mono.error(new BadRequestException("Bu telefon raqam allaqachon ro'yxatdan o'tgan"));
                    BaseUser user = BaseUser.builder()
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .email(request.getEmail())
                            .phone(request.getPhone())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .role(UserRole.STUDENT)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(user);
                })
                .map(user -> toAuthResponse(user, true));
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.error(new UnauthorizedException("Email yoki parol noto'g'ri")))
                .flatMap(user -> {
                    if (request.isMobile()) {
                        return otpService.verifyOtp(request.getEmail(), request.getPassword())
                                .thenReturn(user);
                    } else {
                        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                            return Mono.error(new UnauthorizedException("Email yoki parol noto'g'ri"));
                        }
                        return Mono.just(user);
                    }
                })
                .map(user -> toAuthResponse(user, request.isMobile()));
    }

    @Override
    public Mono<AuthResponse> googleAuth(GoogleAuthRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(user -> toAuthResponse(user, true))
                .switchIfEmpty(
                        // Auto-register if not exists
                        Mono.defer(() -> {
                            BaseUser user = BaseUser.builder()
                                    .firstName(request.getFirstName() != null ? request.getFirstName() : "")
                                    .lastName(request.getLastName() != null ? request.getLastName() : "")
                                    .email(request.getEmail())
                                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                                    .role(UserRole.STUDENT)
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            return userRepository.save(user).map(saved -> toAuthResponse(saved, true));
                        })
                );
    }

    @Override
    public Mono<Void> sendPhoneOtp(String phone, boolean isLogin) {
        String normalizedPhone = normalizePhone(phone);

        Mono<Boolean> check = isLogin
                ? userRepository.existsByPhone(normalizedPhone)
                    .flatMap(exists -> exists ? Mono.just(true) : Mono.error(new BadRequestException("Bu telefon raqam ro'yxatdan o'tmagan!")))
                : userRepository.existsByPhone(normalizedPhone)
                    .flatMap(exists -> exists ? Mono.error(new BadRequestException("Bu telefon raqam allaqachon ro'yxatdan o'tgan!")) : Mono.just(true));

        return check.flatMap(ok -> {
            String otpCode = generateOtp();
            String message = "OAZIS tasdiqlash kodi: " + otpCode;

            return redisTemplate.opsForValue()
                    .set(PHONE_OTP_PREFIX + normalizedPhone, otpCode, Duration.ofMinutes(5))
                    .then(eskizSmsService.sendSms(normalizedPhone, message));
        });
    }

    @Override
    public Mono<AuthResponse> phoneLogin(LoginRequest request) {
        String normalizedPhone = normalizePhone(request.getEmail()); // phone is passed in email field
        String otpCode = request.getPassword();

        // TEST bypass: 55555 master kod — SMS kodsiz ham o'tadi. PRODUCTION'da olib tashlash kerak!
        if ("55555".equals(otpCode)) {
            return redisTemplate.delete(PHONE_OTP_PREFIX + normalizedPhone)
                    .then(userRepository.findByPhone(normalizedPhone))
                    .switchIfEmpty(Mono.error(new UnauthorizedException("Foydalanuvchi topilmadi")))
                    .map(user -> toAuthResponse(user, true));
        }

        return redisTemplate.opsForValue().get(PHONE_OTP_PREFIX + normalizedPhone)
                .switchIfEmpty(Mono.error(new BadRequestException("SMS kod topilmadi yoki muddati o'tgan")))
                .flatMap(storedOtp -> {
                    if (!storedOtp.equals(otpCode)) {
                        return Mono.error(new BadRequestException("SMS kod noto'g'ri"));
                    }
                    return redisTemplate.delete(PHONE_OTP_PREFIX + normalizedPhone)
                            .then(userRepository.findByPhone(normalizedPhone));
                })
                .switchIfEmpty(Mono.error(new UnauthorizedException("Foydalanuvchi topilmadi")))
                .map(user -> toAuthResponse(user, true));
    }

    @Override
    public Mono<AuthResponse> guestLogin() {
        return userRepository.findById(uz.sevenEdu.teacherBot.common.util.GuestUtil.GUEST_USER_ID)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Mehmon profili topilmadi")))
                .map(user -> toAuthResponse(user, true));
    }

    private AuthResponse toAuthResponse(BaseUser user, boolean isMobile) {
        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), user.getRole(), isMobile);
        return AuthResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .token(token)
                .isGuest(Boolean.TRUE.equals(user.getIsGuest()))
                .build();
    }

    private String generateOtp() {
        return String.format("%05d", new Random().nextInt(100000));
    }

    private String normalizePhone(String phone) {
        String digits = phone.replaceAll("[^\\d]", "");
        if (!digits.startsWith("998") && digits.length() == 9) {
            digits = "998" + digits;
        }
        if (digits.startsWith("998")) {
            return "+" + digits;
        }
        return phone;
    }
}
