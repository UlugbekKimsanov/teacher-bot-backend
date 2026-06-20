package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** Darsga biriktirilgan audio kitob (audio fayl). Bir darsda bir nechta bo'lishi mumkin. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("lesson_audiobooks")
public class Audiobook {
    @Id
    private Long id;
    private Long lessonId;
    private String title;
    private String filePath;
    private Integer orderIndex;
    private LocalDateTime createdAt;
}
