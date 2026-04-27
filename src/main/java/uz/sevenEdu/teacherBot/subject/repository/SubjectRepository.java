package uz.sevenEdu.teacherBot.subject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.sevenEdu.teacherBot.subject.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
}
