package uz.sevenEdu.teacherBot.telegram.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.telegram.entity.TelegramSubscriber;

public interface TelegramSubscriberRepository extends ReactiveCrudRepository<TelegramSubscriber, Long> {
    Flux<TelegramSubscriber> findByActiveTrue();
}
