package uz.sevenEdu.teacherBot.course.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("user_courses")
public class UserCourse {
    @Id
    private Long id;
    private Long userId;
    private Long courseId;
    private BigDecimal progress;
    private LocalDateTime enrolledAt;
}
