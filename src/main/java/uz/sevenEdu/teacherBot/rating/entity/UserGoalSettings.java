package uz.sevenEdu.teacherBot.rating.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** Foydalanuvchi o'zi belgilagan kunlik maqsad (daqiqa / so'z). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_goal_settings")
public class UserGoalSettings {
    @Id
    private Long userId;
    private Integer minutesGoal;
    private Integer wordsGoal;
    private LocalDateTime updatedAt;
}
