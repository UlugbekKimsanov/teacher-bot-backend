package uz.sevenEdu.teacherBot.payment.alif.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.sevenEdu.teacherBot.payment.alif.enums.AlifTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("alif_transaction")
public class AlifTransaction {
    @Id
    private Long id;

    /** bePaid tomonidan berilgan tranzaksiya UID */
    private String bepaidUid;

    /** Bizning tizimda: "book_{userId}_{bookId}" */
    private String merchantTransId;

    /** To'lov miqdori (minimal currency unit) */
    private BigDecimal amount;

    private String currency;

    private AlifTransactionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
