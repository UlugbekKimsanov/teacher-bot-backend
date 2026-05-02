package uz.sevenEdu.teacherBot.news.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("news")
public class News {
    @Id
    private Long id;
    private String name;
    private Long imageId;
    private LocalDateTime createdAt;
}
