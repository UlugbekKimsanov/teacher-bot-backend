package uz.sevenEdu.teacherBot.rating.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_achievements")
public class UserAchievement {
    @Id
    private Long id;
    private Long userId;
    private Long achievementId;
    private LocalDateTime unlockedAt;
}
