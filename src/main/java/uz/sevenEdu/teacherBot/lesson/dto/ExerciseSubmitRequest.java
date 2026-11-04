package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ExerciseSubmitRequest {
    /** Eski mijozlar: exerciseId → javob (mashqning birinchi bo'shlig'i uchun). */
    private Map<Long, String> answers;
    /** Yangi mijozlar: blankId → javob (har bo'shliq alohida). */
    private Map<Long, String> blankAnswers;
}
