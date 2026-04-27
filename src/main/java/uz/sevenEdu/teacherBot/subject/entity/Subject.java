package uz.sevenEdu.teacherBot.subject.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.sevenEdu.teacherBot.course.entity.Course;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subjects")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Long imageId;

    @OneToMany(mappedBy = "subject")
    @ToString.Exclude
    @Builder.Default
    private List<Course> courses = new ArrayList<>();
}
