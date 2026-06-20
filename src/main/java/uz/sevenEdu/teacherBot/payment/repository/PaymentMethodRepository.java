package uz.sevenEdu.teacherBot.payment.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.payment.entity.PaymentMethod;

public interface PaymentMethodRepository extends ReactiveCrudRepository<PaymentMethod, Long> {
    Flux<PaymentMethod> findAllByOrderByOrderIndexAsc();
    Flux<PaymentMethod> findByEnabledTrueOrderByOrderIndexAsc();
}
