package uz.sevenEdu.teacherBot.books.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.entity.SaleRecord;

public interface SaleRecordRepository extends ReactiveCrudRepository<SaleRecord, Long> {

    Flux<SaleRecord> findAllByOrderByCreatedAtDesc();

    Flux<SaleRecord> findByUserId(Long userId);

    Flux<SaleRecord> findByPaymentMethod(String paymentMethod);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM sale_records")
    Mono<Long> totalRevenue();

    @Query("SELECT COUNT(*) FROM sale_records")
    Mono<Long> totalSalesCount();
}
