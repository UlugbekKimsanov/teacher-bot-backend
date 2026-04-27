package uz.sevenEdu.teacherBot.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.sevenEdu.teacherBot.auth.dto.AuthResponse;
import uz.sevenEdu.teacherBot.auth.dto.LoginRequest;
import uz.sevenEdu.teacherBot.auth.dto.OtpRequest;
import uz.sevenEdu.teacherBot.auth.dto.RegisterRequest;
import uz.sevenEdu.teacherBot.auth.service.AuthService;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody OtpRequest request) {
        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Tasdiqlash kodi yuborildi", null));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }
}
