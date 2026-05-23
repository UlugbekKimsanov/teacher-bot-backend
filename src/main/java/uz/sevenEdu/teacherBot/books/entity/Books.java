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
    private String category;        // 'digital' or 'print'
    private String description;
    private Integer price;          // in so'm, 0 = free
    private String priceLabel;      // e.g. "45,000 so'm"
    private Boolean isFree;
    private String emoji;
    private Integer coverColor1;    // ARGB int
    private Integer coverColor2;    // ARGB int
    private String pages;           // e.g. "380 bet"
    private Integer pageCount;
    private String format;          // pdf, epub, hardcover, paperback
    private Double rating;
    private Integer reviewCount;
    private String language;
    private String level;
    private String previewPages;    // JSON array string
    private Integer imageId;
    private Integer fileId;
    private String filePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
