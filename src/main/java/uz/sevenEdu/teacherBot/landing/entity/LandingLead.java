package uz.sevenEdu.teacherBot.landing.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import uz.sevenEdu.teacherBot.landing.enums.LeadStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("landing_lead")
public class LandingLead {
    @Id
    private Long id;
    private String name;
    private String phone;
    private LeadStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
