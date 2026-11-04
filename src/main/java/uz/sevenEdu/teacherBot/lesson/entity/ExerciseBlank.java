package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Mashq gapidagi bitta bo'shliq va uning to'g'ri javobi.
 * {@code position} — gapdagi '_' guruhlari tartibi (0 dan).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("exercise_blanks")
public class ExerciseBlank {
    @Id
    private Long id;
    private Long exerciseId;
    private Integer position;
    private String correctAnswer;
}
