package uz.sevenEdu.teacherBot.auth.service;


import uz.sevenEdu.teacherBot.user.dto.AuthResponse;
import uz.sevenEdu.teacherBot.user.dto.LoginRequest;
import uz.sevenEdu.teacherBot.user.dto.RegisterRequest;

public interface AuthService {
    void sendOtp(String email, boolean isLogin);
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
