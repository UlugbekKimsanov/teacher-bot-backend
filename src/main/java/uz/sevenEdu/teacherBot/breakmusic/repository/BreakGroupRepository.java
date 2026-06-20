package uz.sevenEdu.teacherBot.breakmusic.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.breakmusic.entity.BreakGroup;

public interface BreakGroupRepository extends ReactiveCrudRepository<BreakGroup, Long> {
    Flux<BreakGroup> findAllByOrderByOrderIndexAsc();
}
