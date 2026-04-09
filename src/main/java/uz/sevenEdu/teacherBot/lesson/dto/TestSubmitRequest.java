package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Data;
import java.util.Map;

@Data
public class TestSubmitRequest {
    // questionId -> selectedOption ("A", "B", or "C")
    private Map<Long, String> answers;
}
