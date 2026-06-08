package uz.sevenEdu.teacherBot.payment.paynet.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.sevenEdu.teacherBot.payment.paynet.enums.PaynetTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("paynet_transaction")
public class PaynetTransaction {
    @Id
    private Long id;

    /** Paynet tomonidan berilgan tranzaksiya ID */
    private String transactionId;

    /** Bizning tizimda: "book_{userId}_{bookId}" */
    private String merchantTransId;

    /** To'lov miqdori (tiyin) */
    private BigDecimal amount;

    private PaynetTransactionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
