package uz.sevenEdu.teacherBot.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("course_teachers")
public class CourseTeacher {
    @Id
    private Long id;
    private Long courseId;
    private Long teacherId;
}
