package uz.sevenEdu.teacherBot.user.service;

import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.dto.AuthResponse;
import uz.sevenEdu.teacherBot.user.dto.LoginRequest;
import uz.sevenEdu.teacherBot.user.dto.RegisterRequest;

public interface AuthService {
    Mono<AuthResponse> register(RegisterRequest request);
    Mono<AuthResponse> login(LoginRequest request);
}
