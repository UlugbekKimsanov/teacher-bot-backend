package uz.sevenEdu.teacherBot.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.sevenEdu.teacherBot.activity.enums.ActivityType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryDto {
    private ActivityType activityType;
    private Long value;
}
