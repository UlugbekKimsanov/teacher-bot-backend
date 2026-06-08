package uz.sevenEdu.teacherBot.payment.uzum.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.uzum.entity.UzumNasiyaTransaction;

@Repository
public interface UzumNasiyaTransactionRepository extends ReactiveCrudRepository<UzumNasiyaTransaction, Long> {
    Mono<UzumNasiyaTransaction> findByTransId(String transId);
    Mono<UzumNasiyaTransaction> findByMerchantTransId(String merchantTransId);
}
