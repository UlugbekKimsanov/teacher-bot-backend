package uz.sevenEdu.teacherBot.file.dto;

import lombok.Builder;
import lombok.Data;
import uz.sevenEdu.teacherBot.file.entity.FileType;

import java.time.LocalDateTime;

@Data
@Builder
public class FileDto {
    private Long id;
    private String path;
    private String originalName;
    private String mimeType;
    private Long size;
    private Integer duration;
    private FileType type;
    private LocalDateTime createdAt;
}
