package uz.sevenEdu.teacherBot.course.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserCourseProgressDto {
    private Long courseId;
    private String courseName;
    private Long courseImageId;
    private String currentLessonName;
    private BigDecimal progressPercent;
}
