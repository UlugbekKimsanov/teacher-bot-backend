package uz.sevenEdu.teacherBot.subject.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("subjects")
public class Subject {
    @Id
    private Long id;
    private String name;
    private Long imageId;
}
