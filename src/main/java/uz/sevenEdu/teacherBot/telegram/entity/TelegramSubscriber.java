package uz.sevenEdu.teacherBot.telegram.entity;

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
@Table("telegram_subscriber")
public class TelegramSubscriber {
    @Id
    private Long chatId;
    private String username;
    private String fullName;
    private Boolean active;
    private LocalDateTime subscribedAt;
    private LocalDateTime updatedAt;
}
