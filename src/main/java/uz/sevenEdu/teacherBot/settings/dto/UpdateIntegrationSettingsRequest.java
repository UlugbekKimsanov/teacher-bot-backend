package uz.sevenEdu.teacherBot.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateIntegrationSettingsRequest {

    @Size(max = 255)
    private String telegramBotToken;

    @Size(max = 64)
    private String telegramBotUsername;

    private Boolean telegramPollingEnabled;

    @Email
    @Size(max = 255)
    private String eskizEmail;

    @Size(max = 255)
    private String eskizPassword;

    @Size(max = 64)
    private String eskizFrom;
}
