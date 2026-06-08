package uz.sevenEdu.teacherBot.payment.order.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import uz.sevenEdu.teacherBot.payment.order.entity.PaymentOrder;

public interface PaymentOrderRepository extends ReactiveCrudRepository<PaymentOrder, Long> {
}
