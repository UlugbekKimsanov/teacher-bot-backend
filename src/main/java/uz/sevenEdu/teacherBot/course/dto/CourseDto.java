package uz.sevenEdu.teacherBot.course.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CourseDto {
    private Long id;
    private String name;
    private String category;
    private String imageUrl;
    private String backgroundUrl;
    private String flagEmoji;
    private String goal;
    private Boolean isPremium;
    private Integer price;
    private String priceLabel;
    private Boolean isEnrolled;
    private BigDecimal progress;
    private int completedLessons;
    private int lessonCount;
    private int hours;
    private Long currentLessonId;
    private String currentLessonTitle;
    private String currentLessonImage;
    private Integer currentLessonDurationSec;
}
