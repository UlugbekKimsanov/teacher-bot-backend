package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("vocabulary")
public class Vocabulary {
    @Id private Long id;
    private Long lessonId;
    private String phraseUz;
    private String phraseEn;
    private Integer orderIndex;
}
