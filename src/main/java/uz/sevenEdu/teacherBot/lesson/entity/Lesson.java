package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("lessons")
public class Lesson {
    @Id private Long id;
    private Long courseId;
    private String title;
    private String coverImage;
    private String videoUrl;
    private Integer orderIndex;
    private Integer durationSec;
}
