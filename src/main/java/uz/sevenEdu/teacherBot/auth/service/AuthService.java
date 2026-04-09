package uz.sevenEdu.teacherBot.auth.service;

import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.auth.dto.AuthResponse;
import uz.sevenEdu.teacherBot.auth.dto.LoginRequest;
import uz.sevenEdu.teacherBot.auth.dto.RegisterRequest;

public interface AuthService {
    Mono<AuthResponse> register(RegisterRequest request);
    Mono<AuthResponse> login(LoginRequest request);
}
