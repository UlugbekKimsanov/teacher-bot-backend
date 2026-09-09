package uz.sevenEdu.teacherBot.settings.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("integration_settings")
public class IntegrationSettings {

    @Id
    private Long id;
    private String telegramBotToken;
    private String telegramBotUsername;
    private Boolean telegramPollingEnabled;
    private String eskizEmail;
    private String eskizPassword;
    private String eskizFrom;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
