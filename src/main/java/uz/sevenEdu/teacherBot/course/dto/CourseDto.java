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
    /** Saytda (landing) sotuvdami — landing faqat shu kurslarni ko'rsatadi. */
    private Boolean onSale;
    /** Landing kartasidagi o'quvchilar soni (admin qo'lda belgilaydi). */
    private Integer students;
    /** Kurs sotib olinganda sovg'a qilinadigan kitoblar nomlari (bo'lmasa bo'sh ro'yxat). */
    private java.util.List<String> giftBooks;
    /** O'sha sovg'a kitoblarning ID lari (giftBooks bilan bir xil tartibda). */
    private java.util.List<Long> giftBookIds;
}
