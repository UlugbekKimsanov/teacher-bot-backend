package uz.sevenEdu.teacherBot.breakmusic.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("break_tracks")
public class BreakTrack {
    @Id
    private Long id;
    private Long groupId;
    private String title;
    private String filePath;
    private LocalDateTime createdAt;
}
