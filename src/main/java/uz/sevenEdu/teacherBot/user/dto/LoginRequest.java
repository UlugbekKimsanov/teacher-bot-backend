package uz.sevenEdu.teacherBot.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    // Eslatma: telefon login'da bu maydonga telefon raqam yuboriladi,
    // shuning uchun @Email validatsiyasi qo'yilmaydi (aks holda telefon rad etiladi).
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    private boolean mobile;
}
