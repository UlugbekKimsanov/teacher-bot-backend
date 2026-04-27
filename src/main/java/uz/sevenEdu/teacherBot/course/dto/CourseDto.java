package uz.sevenEdu.teacherBot.course.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CourseDto {
    private Long id;
    private String name;
    private String imageUrl;
    private Boolean isEnrolled;
    private BigDecimal progress;
}
