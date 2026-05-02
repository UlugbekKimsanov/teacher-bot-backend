package uz.sevenEdu.teacherBot.activity.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.sevenEdu.teacherBot.activity.enums.ActivityType;
import uz.sevenEdu.teacherBot.activity.enums.MeasureType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("activities")
public class Activity {
    @Id
    private Long id;
    private ActivityType activityType;
    private MeasureType measureType;
    private Long value;
    private Long studentId;
    private LocalDateTime createdAt;
}
