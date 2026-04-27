package uz.sevenEdu.teacherBot.auth.dto;

import lombok.Builder;
import lombok.Data;
import uz.sevenEdu.teacherBot.auth.enums.UserRole;

@Data
@Builder
public class AuthResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private UserRole role;
    private String token;
}
