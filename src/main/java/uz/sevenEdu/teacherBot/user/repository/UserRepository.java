package uz.sevenEdu.teacherBot.user.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.entity.Student;

public interface UserRepository extends ReactiveCrudRepository<Student, Long> {
    Mono<Student> findByPhone(String phone);
    Mono<Boolean> existsByPhone(String phone);
}
