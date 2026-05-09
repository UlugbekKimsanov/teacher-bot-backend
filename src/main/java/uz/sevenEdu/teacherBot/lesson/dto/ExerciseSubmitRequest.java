package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ExerciseSubmitRequest {
    // exerciseId -> user's answer
    private Map<Long, String> answers;
}
