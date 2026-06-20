package uz.sevenEdu.teacherBot.course.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("languages")
public class Language {
    @Id
    private Long id;
    private String name;
    private String backgroundImage;
    private String flagImage;
    private String description;
    private String flagEmoji;
    private Boolean enabled;
    @Transient
    private int courseCount;
    @Transient
    private long studentCount;
}
