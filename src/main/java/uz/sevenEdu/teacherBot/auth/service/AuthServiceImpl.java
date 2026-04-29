package uz.sevenEdu.teacherBot.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.sevenEdu.teacherBot.auth.dto.AuthResponse;
import uz.sevenEdu.teacherBot.auth.dto.LoginRequest;
import uz.sevenEdu.teacherBot.auth.dto.RegisterRequest;
import uz.sevenEdu.teacherBot.auth.entity.User;
import uz.sevenEdu.teacherBot.auth.enums.UserRole;
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
    private final OtpService otpService;

    @Override
    public void sendOtp(String email, boolean isLogin) {
        otpService.sendOtp(email, isLogin);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtpCode());

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Bu telefon raqam allaqachon ro'yxatdan o'tgan");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);
        return toAuthResponse(user, true);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Email yoki parol noto'g'ri"));

        if (request.isMobile()) {
            otpService.verifyOtp(request.getEmail(), request.getPassword());
        } else {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new UnauthorizedException("Email yoki parol noto'g'ri");
            }
        }

        return toAuthResponse(user, request.isMobile());
    }

    private AuthResponse toAuthResponse(User user, boolean isMobile) {
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
