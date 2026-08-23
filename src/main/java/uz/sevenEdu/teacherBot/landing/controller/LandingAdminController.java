package uz.sevenEdu.teacherBot.landing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.landing.dto.LeadDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadStatsDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadUpdateRequest;
import jakarta.validation.Valid;
import uz.sevenEdu.teacherBot.landing.service.LandingService;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.util.List;

/** Admin landing endpointlari — faqat ADMIN roli uchun (AdminController konventsiyasi bo'yicha). */
@RestController
@RequestMapping("/api/admin/landing")
@RequiredArgsConstructor
public class LandingAdminController {

    private final LandingService landingService;
    private final UserRepository userRepository;

    /** Barcha leadlar — sana bo'yicha kamayish tartibida. */
    @GetMapping("/leads")
    public Mono<ApiResponse<List<LeadDto>>> getLeads(Authentication auth) {
        return requireAdmin(auth)
                .thenMany(landingService.getAllLeads())
                .collectList()
                .map(ApiResponse::ok);
    }

    @GetMapping("/leads/stats")
    public Mono<ApiResponse<LeadStatsDto>> getLeadStats(Authentication auth) {
        return requireAdmin(auth)
                .then(landingService.getLeadStats())
                .map(ApiResponse::ok);
    }

    @PatchMapping("/leads/{id}")
    public Mono<ApiResponse<LeadDto>> updateLead(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody LeadUpdateRequest request
    ) {
        return requireAdmin(auth)
                .then(landingService.updateLead(id, request))
                .map(lead -> ApiResponse.ok("Lead holati yangilandi", lead));
    }

    /** To'liq landing kontentini yangilash. */
    @PutMapping("/content")
    public Mono<ApiResponse<JsonNode>> updateContent(Authentication auth, @RequestBody JsonNode content) {
        return requireAdmin(auth)
                .then(landingService.updateContent(content))
                .map(c -> ApiResponse.ok("Kontent yangilandi", c));
    }

    private Mono<Void> requireAdmin(Authentication auth) {
        if (auth == null) return Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi"));
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi")))
                .flatMap(user -> {
                    if (user.getRole() != UserRole.ADMIN) {
                        return Mono.error(new ForbiddenException("Faqat admin uchun"));
                    }
                    return Mono.empty();
                });
    }
}
