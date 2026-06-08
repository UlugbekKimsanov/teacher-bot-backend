package uz.sevenEdu.teacherBot.books.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Har bir sotuv qaydini saqlaydi:
 * kim sotib olgan, qaysi kitob, qancha miqdor, qaysi to'lov usuli.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sale_records")
public class SaleRecord {
    @Id
    private Long id;
    private Long userId;
    private Long bookId;
    private String bookTitle;
    private String buyerName;
    private Integer amount;          // so'm da
    private String paymentMethod;    // click, payme, paynet, uzum_nasiya, alif_nasiya, free
    private String transactionId;    // merchantTransId yoki to'lov tizimi transaction id
    private LocalDateTime createdAt;
}
