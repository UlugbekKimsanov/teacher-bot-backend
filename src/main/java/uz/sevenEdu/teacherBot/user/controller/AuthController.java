package uz.sevenEdu.teacherBot.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.dto.AuthResponse;
import uz.sevenEdu.teacherBot.user.dto.LoginRequest;
import uz.sevenEdu.teacherBot.user.dto.RegisterRequest;
import uz.sevenEdu.teacherBot.user.service.AuthService;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Mono<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request).map(ApiResponse::ok);
    }

    @PostMapping("/login")
    public Mono<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request).map(ApiResponse::ok);
    }
}
