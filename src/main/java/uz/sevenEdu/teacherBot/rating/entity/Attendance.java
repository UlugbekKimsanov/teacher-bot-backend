package uz.sevenEdu.teacherBot.rating.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("attendance")
public class Attendance {
    @Id private Long id;
    private Long userId;
    private Long courseId;
    private LocalDate attendedAt;
}
