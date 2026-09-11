package uz.sevenEdu.teacherBot.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleAuthRequest {
    /** Google Sign-In SDK returned OpenID Connect ID token. */
    @NotBlank(message = "Google ID token yuborilishi shart")
    @Size(max = 4096, message = "Google ID token formati noto'g'ri")
    private String idToken;
}
