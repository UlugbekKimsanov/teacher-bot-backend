package uz.sevenEdu.teacherBot.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PhoneOtpRequest {
    @NotBlank(message = "Telefon raqam kiritilishi shart")
    @Pattern(
            regexp = "^(?:\\+?998)?[\\s()\\-]*\\d{2}(?:[\\s()\\-]*\\d){7}$",
            message = "Telefon raqam formati noto'g'ri"
    )
    private String phone;
    private boolean login;
}
