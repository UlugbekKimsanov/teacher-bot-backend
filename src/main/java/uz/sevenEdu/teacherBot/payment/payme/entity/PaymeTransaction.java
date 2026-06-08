package uz.sevenEdu.teacherBot.payment.payme.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.sevenEdu.teacherBot.payment.payme.enums.PaymeTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payme_transaction")
public class PaymeTransaction {
    @Id
    private Long id;

    /** Payme tomonidan berilgan tranzaksiya ID */
    private String paymeId;

    /** Bizning tizimda: "book_{userId}_{bookId}" */
    private String merchantTransId;

    /** To'lov miqdori (tiyin — 1 so'm = 100 tiyin) */
    private BigDecimal amount;

    private PaymeTransactionStatus status;

    /** Payme tomonidan berilgan vaqt (ms) */
    private Long createTime;
    private Long performTime;
    private Long cancelTime;

    /** Bekor qilish sababi */
    private Integer cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
