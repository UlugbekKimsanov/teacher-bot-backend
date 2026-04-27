package uz.sevenEdu.teacherBot.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String phone;
    @NotBlank
    private String password;
    @NotBlank
    private String email;
    @NotBlank
    private String otpCode;
}
