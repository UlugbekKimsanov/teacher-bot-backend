package uz.sevenEdu.teacherBot.rating.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("achievements")
public class Achievement {
    @Id
    private Long id;
    private String code;
    private String title;
    private String description;
    private String icon;
    private Integer bonusPoints;
    private String conditionType;
    private Integer conditionValue;
}
