package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Builder;
import lombok.Data;
import uz.sevenEdu.teacherBot.lesson.entity.ExerciseBlank;

/** Mashqdagi bitta bo'shliq (student va admin javoblarida bir xil). */
@Data
@Builder
public class ExerciseBlankDto {
    private Long id;
    private Integer position;
    private String correctAnswer;

    public static ExerciseBlankDto from(ExerciseBlank b) {
        return ExerciseBlankDto.builder()
                .id(b.getId())
                .position(b.getPosition())
                .correctAnswer(b.getCorrectAnswer())
                .build();
    }
}
