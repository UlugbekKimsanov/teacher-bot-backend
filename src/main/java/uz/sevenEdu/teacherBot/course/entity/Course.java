package uz.sevenEdu.teacherBot.course.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.sevenEdu.teacherBot.file.entity.FileEntity;
import uz.sevenEdu.teacherBot.subject.entity.Subject;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    @ToString.Exclude
    private FileEntity image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    @ToString.Exclude
    private Subject subject;
}
