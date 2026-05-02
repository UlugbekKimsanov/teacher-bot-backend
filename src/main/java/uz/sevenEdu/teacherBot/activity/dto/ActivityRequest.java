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
    @NotNull
    private Long studentId;
}
