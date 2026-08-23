package uz.sevenEdu.teacherBot.landing.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LeadRequest {
    @Size(max = 255)
    private String name;

    @NotBlank
    @Pattern(regexp = "^\\+998 \\d{2} \\d{3} \\d{2} \\d{2}$", message = "Telefon raqami +998 XX XXX XX XX formatida bo'lishi kerak")
    private String phone;
}
