package uz.sevenEdu.teacherBot.payment.alif.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.alif.entity.AlifTransaction;

@Repository
public interface AlifTransactionRepository extends ReactiveCrudRepository<AlifTransaction, Long> {
    Mono<AlifTransaction> findByBepaidUid(String bepaidUid);
    Mono<AlifTransaction> findByMerchantTransId(String merchantTransId);
}
