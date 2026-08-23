package uz.sevenEdu.teacherBot.landing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeadStatsDto {
    private long total;
    private long newRequests;
    private long purchased;
    private long rejected;
}
