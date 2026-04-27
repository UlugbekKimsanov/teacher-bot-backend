package uz.sevenEdu.teacherBot.auth.service;

import uz.sevenEdu.teacherBot.auth.dto.AuthResponse;
import uz.sevenEdu.teacherBot.auth.dto.LoginRequest;
import uz.sevenEdu.teacherBot.auth.dto.RegisterRequest;

public interface AuthService {
    void sendOtp(String email);
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
