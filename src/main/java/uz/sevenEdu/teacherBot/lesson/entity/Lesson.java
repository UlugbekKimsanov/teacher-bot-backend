package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("lessons")
public class Lesson {
    @Id
    private Long id;
    private Long courseId;
    private String name;
    private String description;
    private Integer orderIndex;
    private Integer durationSec;
    private String coverImage;
}
