package uz.sevenEdu.teacherBot.course.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.entity.Language;

public interface LanguageRepository extends ReactiveCrudRepository<Language, Long> {
    Mono<Language> findByName(String name);
}
