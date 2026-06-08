package uz.sevenEdu.teacherBot.payment.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class PaymentOrder {

    @Id
    private Long id;

    private Long userId;
    private Long productId;
    private String productType; // COURSE | BOOK

    private BigDecimal amount;

    @Builder.Default
    private String status = "PENDING"; // PENDING, PAID, FAILED, CANCELLED

    private String paymentMethod;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
