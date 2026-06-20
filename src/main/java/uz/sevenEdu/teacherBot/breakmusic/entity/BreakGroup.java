package uz.sevenEdu.teacherBot.breakmusic.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("break_groups")
public class BreakGroup {
    @Id
    private Long id;
    private String name;
    private String icon;
    private String backgroundImage;
    private Integer orderIndex;

    @Transient
    private List<BreakTrack> tracks;
}
