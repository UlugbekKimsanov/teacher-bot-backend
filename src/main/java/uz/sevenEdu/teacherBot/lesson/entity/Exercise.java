package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("exercises")
public class Exercise {
    @Id
    private Long id;
    private Long lessonId;
    private String name;
    private Integer orderIndex;
    /** Gap; har tushib qolgan so'z o'rnida '_' (bir nechta bo'lishi mumkin). */
    private String sentence;
    /** Chalg'ituvchi so'zlar, vergul bilan. To'g'ri javoblar — {@link ExerciseBlank} jadvalida. */
    private String options;
}
