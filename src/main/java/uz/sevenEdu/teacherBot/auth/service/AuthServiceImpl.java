package uz.sevenEdu.teacherBot.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.auth.dto.AuthResponse;
import uz.sevenEdu.teacherBot.auth.dto.LoginRequest;
import uz.sevenEdu.teacherBot.auth.dto.RegisterRequest;
import uz.sevenEdu.teacherBot.auth.entity.User;
import uz.sevenEdu.teacherBot.auth.repository.UserRepository;
import uz.sevenEdu.teacherBot.auth.security.JwtUtil;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.existsByPhone(request.getPhone())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BadRequestException("Bu telefon raqam allaqachon ro'yxatdan o'tgan"));
                    }
                    User user = User.builder()
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .phone(request.getPhone())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .role("STUDENT")
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(user);
                })
                .map(this::toAuthResponse);
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByPhone(request.getPhone())
                .switchIfEmpty(Mono.error(new UnauthorizedException("Telefon raqam yoki parol noto'g'ri")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        return Mono.error(new UnauthorizedException("Telefon raqam yoki parol noto'g'ri"));
                    }
                    return Mono.just(user);
                })
                .map(this::toAuthResponse);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), user.getRole());
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
