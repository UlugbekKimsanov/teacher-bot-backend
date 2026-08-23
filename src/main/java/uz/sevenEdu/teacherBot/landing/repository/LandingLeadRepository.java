package uz.sevenEdu.teacherBot.landing.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.landing.entity.LandingLead;
import uz.sevenEdu.teacherBot.landing.enums.LeadStatus;

public interface LandingLeadRepository extends ReactiveCrudRepository<LandingLead, Long> {
    Flux<LandingLead> findAllByOrderByCreatedAtDesc();
    Mono<Long> countByStatus(LeadStatus status);
}
