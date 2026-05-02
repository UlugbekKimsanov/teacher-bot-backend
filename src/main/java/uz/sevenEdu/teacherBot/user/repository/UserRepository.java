package uz.sevenEdu.teacherBot.user.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;

public interface UserRepository extends ReactiveCrudRepository<BaseUser, Long> {
    Mono<BaseUser> findByPhone(String phone);
    Mono<BaseUser> findByEmail(String email);
    Mono<Boolean> existsByPhone(String phone);
    Mono<Boolean> existsByEmail(String email);
}
