package uz.sevenEdu.teacherBot.books.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BooksDto {
    private Long id;
    private String title;
    private String author;
    private String category;
    private String description;
    private Integer price;
    private String priceLabel;
    private Boolean isFree;
    private String emoji;
    private List<Integer> coverColors;
    private String pages;
    private Integer pageCount;
    private String format;
    private Double rating;
    private Integer reviewCount;
    private String language;
    private String level;
    private List<String> previewPages;
    private Boolean isPurchased;
    private Boolean inLibrary;
    private Integer readPage;
    private Integer totalPages;
    private Integer imageId;
    private Integer fileId;
    private String fileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
