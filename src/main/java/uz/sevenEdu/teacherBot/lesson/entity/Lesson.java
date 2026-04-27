package uz.sevenEdu.teacherBot.lesson.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.sevenEdu.teacherBot.course.entity.Course;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @ToString.Exclude
    private Course course;

    private String name;
    private String description;
    private Integer orderIndex;
    private Integer durationSec;
}
