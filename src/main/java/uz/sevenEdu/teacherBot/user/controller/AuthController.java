package uz.sevenEdu.teacherBot.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.dto.*;
import uz.sevenEdu.teacherBot.user.service.AuthService;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/send-otp")
    public Mono<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpRequest request) {
        return authService.sendOtp(request.getEmail(), request.isLogin())
                .then(Mono.just(ApiResponse.ok("Tasdiqlash kodi yuborildi", null)));
    }

    @PostMapping("/register")
    public Mono<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request).map(ApiResponse::ok);
    }

    @PostMapping("/login")
    public Mono<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request).map(ApiResponse::ok);
    }

    @PostMapping("/google")
    public Mono<ApiResponse<AuthResponse>> googleAuth(@Valid @RequestBody GoogleAuthRequest request) {
        return authService.googleAuth(request).map(ApiResponse::ok);
    }

    /** Mehmon (guest) sifatida kirish — bepul kontentni ko'rish uchun token. */
    @PostMapping("/guest")
    public Mono<ApiResponse<AuthResponse>> guest() {
        return authService.guestLogin().map(ApiResponse::ok);
    }

    @PostMapping("/phone/send-otp")
    public Mono<ApiResponse<Void>> sendPhoneOtp(@Valid @RequestBody PhoneOtpRequest request) {
        return authService.sendPhoneOtp(request.getPhone(), request.isLogin())
                .then(Mono.just(ApiResponse.ok("SMS kod yuborildi", null)));
    }

    @PostMapping("/phone/login")
    public Mono<ApiResponse<AuthResponse>> phoneLogin(@Valid @RequestBody LoginRequest request) {
        return authService.phoneLogin(request).map(ApiResponse::ok);
    }
}
