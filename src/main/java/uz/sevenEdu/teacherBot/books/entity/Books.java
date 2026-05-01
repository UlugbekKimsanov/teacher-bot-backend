package uz.sevenEdu.teacherBot.books.entity;

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
@Table("books")
public class Books {
    @Id
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
