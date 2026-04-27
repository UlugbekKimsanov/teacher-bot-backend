package uz.sevenEdu.teacherBot.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.sevenEdu.teacherBot.course.entity.Course;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySubjectId(Long subjectId);
}
