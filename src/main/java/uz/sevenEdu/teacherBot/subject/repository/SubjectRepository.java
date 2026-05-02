package uz.sevenEdu.teacherBot.subject.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import uz.sevenEdu.teacherBot.subject.entity.Subject;

public interface SubjectRepository extends ReactiveCrudRepository<Subject, Long> {
}
