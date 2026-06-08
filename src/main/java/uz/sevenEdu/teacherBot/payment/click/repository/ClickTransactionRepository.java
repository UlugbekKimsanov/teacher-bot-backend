package uz.sevenEdu.teacherBot.payment.click.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.click.entity.ClickTransaction;

public interface ClickTransactionRepository extends ReactiveCrudRepository<ClickTransaction, Long> {
    Mono<ClickTransaction> findByClickTransId(Long clickTransId);
    Mono<ClickTransaction> findByMerchantTransId(String merchantTransId);
    Mono<ClickTransaction> findByMerchantPrepareId(Long merchantPrepareId);
}
