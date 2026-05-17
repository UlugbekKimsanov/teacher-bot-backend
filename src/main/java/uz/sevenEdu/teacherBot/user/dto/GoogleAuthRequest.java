package uz.sevenEdu.teacherBot.user.dto;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String email;
    private String firstName;
    private String lastName;
}
