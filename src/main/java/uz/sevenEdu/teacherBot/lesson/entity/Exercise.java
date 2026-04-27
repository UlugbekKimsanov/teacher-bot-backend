package uz.sevenEdu.teacherBot.lesson.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "exercises")
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long lessonId;
    private String name;
    private Integer orderIndex;
    private String sentence;
    private String options;
    private String correctAnswer;
}
