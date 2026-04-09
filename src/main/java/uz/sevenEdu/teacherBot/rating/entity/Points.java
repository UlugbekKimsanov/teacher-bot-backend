package uz.sevenEdu.teacherBot.rating.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("points")
public class Points {
    @Id private Long id;
    private Long userId;
    private String activity;
    private Integer amount;
    private LocalDateTime createdAt;
}
