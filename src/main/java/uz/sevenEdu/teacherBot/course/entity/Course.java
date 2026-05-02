package uz.sevenEdu.teacherBot.course.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("courses")
public class Course {
    @Id
    private Long id;
    private String name;
    private String category;
    private String flagEmoji;
    private Integer hours;
    private Integer lessonCount;
    private String goal;
    private Boolean isPremium;
    private String coverImage;
    private Long languageId;
    private LocalDateTime createdAt;
}
