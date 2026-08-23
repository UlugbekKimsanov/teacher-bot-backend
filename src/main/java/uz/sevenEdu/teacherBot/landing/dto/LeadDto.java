package uz.sevenEdu.teacherBot.landing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import uz.sevenEdu.teacherBot.landing.enums.LeadStatus;

@Data
@Builder
public class LeadDto {
    private Long id;
    private String name;
    private String phone;
    private LeadStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
