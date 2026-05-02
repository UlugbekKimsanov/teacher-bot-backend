package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("exercises")
public class Exercise {
    @Id
    private Long id;
    private Long lessonId;
    private String name;
    private Integer orderIndex;
    private String sentence;
    private String options;
    private String correctAnswer;
}
