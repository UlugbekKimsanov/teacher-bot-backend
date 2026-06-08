package uz.sevenEdu.teacherBot.payment.paynet.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.paynet.entity.PaynetTransaction;

import java.time.LocalDateTime;

@Repository
public interface PaynetTransactionRepository extends ReactiveCrudRepository<PaynetTransaction, Long> {
    Mono<PaynetTransaction> findByTransactionId(String transactionId);
    Mono<PaynetTransaction> findByMerchantTransId(String merchantTransId);
    Flux<PaynetTransaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
