package uz.sevenEdu.teacherBot.payment.click.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.sevenEdu.teacherBot.payment.click.enums.ClickTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("click_transaction")
public class ClickTransaction {

    @Id
    private Long id;

    /** Click tomonidan berilgan tranzaksiya ID */
    private Long clickTransId;

    /** Click paydoc ID */
    private Long clickPaydocId;

    /** Bizning tizimda to'lovning ID (merchant_trans_id) */
    private String merchantTransId;

    /** Prepare bosqichida saqlanadigan ID (merchant_prepare_id) */
    private Long merchantPrepareId;

    /** To'lov miqdori */
    private BigDecimal amount;

    private ClickTransactionStatus status;

    private String signTime;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
