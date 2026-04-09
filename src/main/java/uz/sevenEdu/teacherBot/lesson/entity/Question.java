package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("questions")
public class Question {
    @Id private Long id;
    private Long testId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String correctOption;
    private Integer orderIndex;
}
