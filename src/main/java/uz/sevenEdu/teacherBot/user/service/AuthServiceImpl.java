package uz.sevenEdu.teacherBot.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.dto.AuthResponse;
import uz.sevenEdu.teacherBot.user.dto.LoginRequest;
import uz.sevenEdu.teacherBot.user.dto.RegisterRequest;
import uz.sevenEdu.teacherBot.user.entity.Student;
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

    @Override
    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.existsByPhone(request.getPhone())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BadRequestException("Bu telefon raqam allaqachon ro'yxatdan o'tgan"));
                    }
                    Student student = Student.builder()
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .phone(request.getPhone())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .role(UserRole.STUDENT)
                            .ball(0L)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(student);
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

    private AuthResponse toAuthResponse(Student student) {
        String token = jwtUtil.generateToken(student.getId(), student.getPhone(), student.getRole().name());
        return AuthResponse.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .phone(student.getPhone())
                .role(student.getRole().name())
                .token(token)
                .build();
    }
}
