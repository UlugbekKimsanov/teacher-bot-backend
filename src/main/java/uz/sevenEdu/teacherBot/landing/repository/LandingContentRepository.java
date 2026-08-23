package uz.sevenEdu.teacherBot.landing.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import uz.sevenEdu.teacherBot.landing.entity.LandingContent;

public interface LandingContentRepository extends ReactiveCrudRepository<LandingContent, Long> {
}
