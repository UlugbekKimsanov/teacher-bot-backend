package uz.sevenEdu.teacherBot.landing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uz.sevenEdu.teacherBot.landing.enums.LeadStatus;

@Data
public class LeadUpdateRequest {
    @NotNull
    private LeadStatus status;

    @Size(max = 1000)
    private String rejectionReason;
}
