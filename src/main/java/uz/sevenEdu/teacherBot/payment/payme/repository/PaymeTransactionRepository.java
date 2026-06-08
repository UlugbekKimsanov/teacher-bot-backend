package uz.sevenEdu.teacherBot.payment.payme.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.payme.entity.PaymeTransaction;

@Repository
public interface PaymeTransactionRepository extends ReactiveCrudRepository<PaymeTransaction, Long> {
    Mono<PaymeTransaction> findByPaymeId(String paymeId);
    Mono<PaymeTransaction> findByMerchantTransId(String merchantTransId);
    Flux<PaymeTransaction> findByCreateTimeBetween(Long from, Long to);
}
