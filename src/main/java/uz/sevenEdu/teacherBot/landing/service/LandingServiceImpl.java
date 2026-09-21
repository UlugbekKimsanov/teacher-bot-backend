package uz.sevenEdu.teacherBot.landing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.landing.dto.LeadDto;
import uz.sevenEdu.teacherBot.landing.dto.LandingContentDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadRequest;
import uz.sevenEdu.teacherBot.landing.dto.LeadStatsDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadUpdateRequest;
import uz.sevenEdu.teacherBot.landing.entity.LandingLead;
import uz.sevenEdu.teacherBot.landing.repository.LandingContentRepository;
import uz.sevenEdu.teacherBot.landing.repository.LandingLeadRepository;
import uz.sevenEdu.teacherBot.landing.enums.LeadStatus;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.telegram.TelegramBotService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LandingServiceImpl implements LandingService {

    private static final Long CONTENT_ID = 1L;

    private final LandingLeadRepository leadRepository;
    private final LandingContentRepository contentRepository;
    private final LandingContentNormalizer contentNormalizer;
    private final TelegramBotService telegramBotService;

    @Override
    public Mono<LeadDto> createLead(LeadRequest request) {
        LandingLead lead = LandingLead.builder()
                .name(cleanOptional(request.getName()))
                .phone(request.getPhone().trim())
                .status(LeadStatus.NEW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return leadRepository.save(lead)
                .flatMap(saved -> telegramBotService.notifySubscribers(newLeadMessage(saved)).thenReturn(saved))
                .map(this::toDto);
    }

    @Override
    public Flux<LeadDto> getAllLeads() {
        return leadRepository.findAllByOrderByCreatedAtDesc().map(this::toDto);
    }

    @Override
    public Mono<LeadDto> updateLead(Long id, LeadUpdateRequest request) {
        String reason = cleanOptional(request.getRejectionReason());
        if (request.getStatus() == LeadStatus.REJECTED && reason == null) {
            return Mono.error(new BadRequestException("Rad etish sababini kiriting"));
        }
        return leadRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Lead topilmadi")))
                .flatMap(lead -> {
                    lead.setStatus(request.getStatus());
                    lead.setRejectionReason(request.getStatus() == LeadStatus.REJECTED ? reason : null);
                    lead.setUpdatedAt(LocalDateTime.now());
                    return leadRepository.save(lead);
                })
                .map(this::toDto);
    }

    @Override
    public Mono<LeadStatsDto> getLeadStats() {
        return Mono.zip(
                leadRepository.count(),
                leadRepository.countByStatus(LeadStatus.NEW),
                leadRepository.countByStatus(LeadStatus.PURCHASED),
                leadRepository.countByStatus(LeadStatus.REJECTED)
        ).map(values -> LeadStatsDto.builder()
                .total(values.getT1())
                .newRequests(values.getT2())
                .purchased(values.getT3())
                .rejected(values.getT4())
                .build());
    }

    @Override
    public Mono<LandingContentDto> getContent() {
        return contentRepository.findById(CONTENT_ID)
                .map(c -> contentNormalizer.normalize(c.getContent()))
                .switchIfEmpty(Mono.fromSupplier(contentNormalizer::defaultContent));
    }

    @Override
    public Mono<LandingContentDto> updateContent(LandingContentDto content) {
        String json = contentNormalizer.write(content);
        return contentRepository.upsert(CONTENT_ID, json)
                .map(c -> contentNormalizer.normalize(c.getContent()));
    }

    private LeadDto toDto(LandingLead l) {
        return LeadDto.builder()
                .id(l.getId())
                .name(l.getName())
                .phone(l.getPhone())
                .status(l.getStatus())
                .rejectionReason(l.getRejectionReason())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String newLeadMessage(LandingLead lead) {
        String name = lead.getName() == null ? "Ko'rsatilmagan" : escapeHtml(lead.getName());
        return "<b>Yangi landing arizasi</b>\n\n"
                + "Ism: " + name + "\n"
                + "Telefon: <code>" + escapeHtml(lead.getPhone()) + "</code>\n"
                + "Lead ID: #" + lead.getId();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

}
