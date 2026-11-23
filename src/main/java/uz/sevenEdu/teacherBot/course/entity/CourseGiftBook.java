package uz.sevenEdu.teacherBot.course.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** Kursga biriktirilgan sovg'a kitob (kurs sotib olinganda kutubxonaga qo'shiladi). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("course_gift_books")
public class CourseGiftBook {
    @Id
    private Long id;
    private Long courseId;
    private Long bookId;
    private LocalDateTime createdAt;
}
