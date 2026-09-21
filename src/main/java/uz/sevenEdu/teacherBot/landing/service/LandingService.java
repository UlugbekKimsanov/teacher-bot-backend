package uz.sevenEdu.teacherBot.landing.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.landing.dto.LandingContentDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadRequest;
import uz.sevenEdu.teacherBot.landing.dto.LeadStatsDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadUpdateRequest;

public interface LandingService {
    Mono<LeadDto> createLead(LeadRequest request);

    Flux<LeadDto> getAllLeads();

    Mono<LeadDto> updateLead(Long id, LeadUpdateRequest request);

    Mono<LeadStatsDto> getLeadStats();

    Mono<LandingContentDto> getContent();

    Mono<LandingContentDto> updateContent(LandingContentDto content);
}
