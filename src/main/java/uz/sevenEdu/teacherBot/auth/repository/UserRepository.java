package uz.sevenEdu.teacherBot.auth.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.auth.entity.User;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByPhone(String phone);
    Mono<Boolean> existsByPhone(String phone);
}
