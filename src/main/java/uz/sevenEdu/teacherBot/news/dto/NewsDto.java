package uz.sevenEdu.teacherBot.news.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NewsDto {
    private Long id;
    private String name;
    private Long imageId;
    private LocalDateTime createdAt;
}
