package uz.sevenEdu.teacherBot.rating.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("certificates")
public class Certificate {
    @Id
    private Long id;
    private Long userId;
    private Long courseId;
    private LocalDateTime issuedAt;
}
