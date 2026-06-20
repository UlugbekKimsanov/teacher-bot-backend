package uz.sevenEdu.teacherBot.payment.order.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.order.entity.PaymentOrder;

import java.math.BigDecimal;

public interface PaymentOrderRepository extends ReactiveCrudRepository<PaymentOrder, Long> {

    /** To'langan kurs/kitob buyurtmalari summasi (so'm) */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM orders WHERE status = 'PAID'")
    Mono<BigDecimal> totalPaidAmount();

    /** To'langan buyurtmalar — yangidan eskiga */
    @Query("SELECT * FROM orders WHERE status = 'PAID' ORDER BY created_at DESC")
    Flux<PaymentOrder> findPaidOrders();
}
