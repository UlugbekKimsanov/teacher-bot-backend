package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("tests")
public class TestEntity {
    @Id
    private Long id;
    private Long lessonId;
    private String name;
}
