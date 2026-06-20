package uz.sevenEdu.teacherBot.course.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("courses")
public class Course {
    @Id
    private Long id;
    private String name;
    private Long imageId;
    private Long subjectId;
    private Long languageId;
    private String coverImage;
    private String backgroundImage;
    private String flagEmoji;
    private String goal;
    private Boolean isPremium;
    private Integer price;        // so'mda (premium kurslar uchun)
    private String priceLabel;    // masalan "199 000 so'm"
    private Integer orderIndex;   // ko'rsatish tartibi (admin sozlaydi)

    // Hisoblangan maydonlar (DB ustuni emas)
    @Transient
    private Long studentCount;
    @Transient
    private Long lessonCount;
}
