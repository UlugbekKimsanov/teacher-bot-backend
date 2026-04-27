package uz.sevenEdu.teacherBot.file.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "files")
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String path;
    private String originalName;
    private String mimeType;
    private Long size;
    private Long lessonId;
    private Integer duration;

    @Enumerated(EnumType.STRING)
    private FileType type;

    private LocalDateTime createdAt;
}
