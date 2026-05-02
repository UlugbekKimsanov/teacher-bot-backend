package uz.sevenEdu.teacherBot.user.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserProfileDto {
    private String firstName;
    private String lastName;
    private String address;
    private Integer coursesCount;
    private Integer lessonsCount;
    private BigDecimal completionPercent;
}
