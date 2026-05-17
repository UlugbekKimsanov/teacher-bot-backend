package uz.sevenEdu.teacherBot.user.service;

import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.dto.*;

public interface AuthService {
    Mono<Void> sendOtp(String email, boolean isLogin);
    Mono<AuthResponse> register(RegisterRequest request);
    Mono<AuthResponse> login(LoginRequest request);
    Mono<AuthResponse> googleAuth(GoogleAuthRequest request);
    Mono<Void> sendPhoneOtp(String phone, boolean isLogin);
    Mono<AuthResponse> phoneLogin(LoginRequest request);
}
