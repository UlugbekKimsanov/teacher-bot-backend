package uz.sevenEdu.teacherBot.activity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uz.sevenEdu.teacherBot.activity.enums.ActivityType;
import uz.sevenEdu.teacherBot.activity.enums.MeasureType;

@Data
public class ActivityRequest {
    @NotNull
    private ActivityType activityType;
    @NotNull
    private MeasureType measureType;
    @NotNull
    private Long value;
    /** Clientdan kelgani e'tiborga olinmaydi — server tokendan to'ldiradi. */
    private Long studentId;
}
