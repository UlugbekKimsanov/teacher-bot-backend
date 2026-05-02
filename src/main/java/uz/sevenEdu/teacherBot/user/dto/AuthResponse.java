package uz.sevenEdu.teacherBot.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private String token;
}
