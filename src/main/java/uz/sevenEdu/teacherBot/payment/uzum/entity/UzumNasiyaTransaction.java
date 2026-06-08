package uz.sevenEdu.teacherBot.payment.uzum.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.sevenEdu.teacherBot.payment.uzum.enums.UzumNasiyaTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("uzum_nasiya_transaction")
public class UzumNasiyaTransaction {
    @Id
    private Long id;

    /** Uzum Nasiya tomonidan berilgan tranzaksiya ID */
    private String transId;

    /** Bizning tizimda: "book_{userId}_{bookId}" */
    private String merchantTransId;

    /** To'lov miqdori (tiyin) */
    private BigDecimal amount;

    private UzumNasiyaTransactionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
