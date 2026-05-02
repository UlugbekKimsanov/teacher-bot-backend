package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.entity.Certificate;

public interface CertificateRepository extends ReactiveCrudRepository<Certificate, Long> {
    Flux<Certificate> findByUserId(Long userId);
    Mono<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);
}
