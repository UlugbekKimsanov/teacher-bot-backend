package uz.sevenEdu.teacherBot.lesson.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vocabulary")
public class Vocabulary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long lessonId;
    private String translationUz;
    private String translationTarget;
    private Integer orderIndex;
}
