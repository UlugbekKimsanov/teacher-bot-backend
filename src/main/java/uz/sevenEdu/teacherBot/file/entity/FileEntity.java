package uz.sevenEdu.teacherBot.file.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("files")
public class FileEntity {
    @Id
    private Long id;
    private String path;
    private String originalName;
    private String mimeType;
    private Long size;
    private Long lessonId;
    private Integer duration;
    private FileType type;
    private LocalDateTime createdAt;
}
