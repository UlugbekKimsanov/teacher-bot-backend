package uz.sevenEdu.teacherBot.course.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("courses")
public class Course {
    @Id
    private Long id;
    private String name;
    private Long imageId;
    private Long subjectId;
    private Long languageId;
    private String coverImage;
    private String flagEmoji;
    private String goal;
    private Boolean isPremium;
}
