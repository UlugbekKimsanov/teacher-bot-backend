package uz.sevenEdu.teacherBot.landing.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;
import uz.sevenEdu.teacherBot.landing.dto.LeadDto;
import uz.sevenEdu.teacherBot.landing.dto.LandingContentDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadStatsDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadUpdateRequest;
import jakarta.validation.Valid;
import uz.sevenEdu.teacherBot.landing.repository.LandingLeadRepository;
import uz.sevenEdu.teacherBot.landing.service.LandingService;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.util.List;
import java.util.Map;

/** Admin landing endpointlari — faqat ADMIN roli uchun (AdminController konventsiyasi bo'yicha). */
@RestController
@RequestMapping("/api/admin/landing")
@RequiredArgsConstructor
public class LandingAdminController {

    private final LandingService landingService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final LandingLeadRepository landingLeadRepository;

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
    public Mono<ApiResponse<LandingContentDto>> updateContent(
            Authentication auth,
            @Valid @RequestBody LandingContentDto content
    ) {
        return requireAdmin(auth)
                .then(landingService.updateContent(content))
                .map(c -> ApiResponse.ok("Kontent yangilandi", c));
    }

    /** Landing sahifasi uchun rasm yuklash — natija: { "path": "landing/..." }. */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Mono<ApiResponse<Map<String, String>>> uploadImage(
            Authentication auth,
            @RequestPart("file") FilePart file
    ) {
        return requireAdmin(auth)
                .then(Mono.defer(() -> fileStorageService.saveLandingImage(file)))
                .map(path -> ApiResponse.ok(Map.of("path", path)));
    }

    /** Lead o'chirish. */
    @DeleteMapping("/leads/{id}")
    public Mono<ApiResponse<Void>> deleteLead(Authentication auth, @PathVariable Long id) {
        return requireAdmin(auth)
                .then(landingLeadRepository.deleteById(id))
                .thenReturn(ApiResponse.ok("Lead o'chirildi", (Void) null));
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
