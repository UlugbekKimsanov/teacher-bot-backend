package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("teacher_questions")
public class TeacherQuestion {
    @Id
    private Long id;
    private Long userId;
    private Long lessonId;
    private String question;
    private LocalDateTime createdAt;
}
