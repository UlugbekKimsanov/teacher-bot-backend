package uz.sevenEdu.teacherBot.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.dto.AuthResponse;
import uz.sevenEdu.teacherBot.user.dto.LoginRequest;
import uz.sevenEdu.teacherBot.user.dto.RegisterRequest;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;
import uz.sevenEdu.teacherBot.user.security.JwtUtil;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

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

    private AuthResponse toAuthResponse(BaseUser user, boolean isMobile) {
        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), user.getRole(), isMobile);
        return AuthResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .token(token)
                .build();
    }
}
