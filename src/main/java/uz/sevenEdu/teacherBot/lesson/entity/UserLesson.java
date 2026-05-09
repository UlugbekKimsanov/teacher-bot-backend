package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_lessons")
public class UserLesson {
    @Id
    private Long id;
    private Long userId;
    private Long lessonId;
    private Boolean isCompleted;
    private Integer vocabScore;
    private Integer testScore;
    private Integer exerciseScore;
    private LocalDateTime completedAt;
}
