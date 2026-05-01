package uz.sevenEdu.teacherBot.books.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BooksDto {
    private Long id;
    private String title;
    private String author;
    private String category;
    private String description;
    private Integer price;
    private Integer imageId;
    private Integer fileId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
