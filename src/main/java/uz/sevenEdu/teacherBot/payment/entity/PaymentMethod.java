package uz.sevenEdu.teacherBot.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_methods")
public class PaymentMethod {
    @Id
    private Long id;
    private String code;        // click, payme, paynet, uzumnasiya, alifnasiya
    private String name;
    private Boolean isNasiya;   // nasiya (bo'lib to'lash) usulimi
    private Boolean enabled;
    private Integer orderIndex;
}
