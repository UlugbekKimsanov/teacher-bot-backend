package uz.sevenEdu.teacherBot.notification.entity;

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
@Table("notifications")
public class Notification {
    @Id
    private Long id;
    private Long userId;
    private String title;
    private String body;
    private String type;
    private Long refId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
