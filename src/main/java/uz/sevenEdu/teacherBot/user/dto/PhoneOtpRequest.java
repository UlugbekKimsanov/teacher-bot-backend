package uz.sevenEdu.teacherBot.user.dto;

import lombok.Data;

@Data
public class PhoneOtpRequest {
    private String phone;
    private boolean login;
}
