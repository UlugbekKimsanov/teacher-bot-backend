package uz.sevenEdu.teacherBot.breakmusic.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.breakmusic.entity.BreakTrack;

public interface BreakTrackRepository extends ReactiveCrudRepository<BreakTrack, Long> {
    Flux<BreakTrack> findByGroupIdOrderById(Long groupId);
}
