package uz.sevenEdu.teacherBot.settings.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.settings.dto.IntegrationSettingsResponse;
import uz.sevenEdu.teacherBot.settings.dto.UpdateIntegrationSettingsRequest;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final IntegrationSettingsService settingsService;
    private final UserRepository userRepository;

    @GetMapping
    public Mono<ApiResponse<IntegrationSettingsResponse>> getSettings(Authentication auth) {
        return requireAdmin(auth)
                .then(settingsService.getAdminSettings())
                .map(ApiResponse::ok);
    }

    @PutMapping
    public Mono<ApiResponse<IntegrationSettingsResponse>> updateSettings(
            Authentication auth,
            @Valid @RequestBody UpdateIntegrationSettingsRequest request
    ) {
        return requireAdmin(auth)
                .then(Mono.defer(() -> settingsService.update(request, principalId(auth))))
                .map(settings -> ApiResponse.ok("Sozlamalar saqlandi", settings));
    }

    private Mono<Void> requireAdmin(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            return Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi"));
        }
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi")))
                .flatMap(user -> {
                    if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.HEAD_ADMIN) {
                        return Mono.error(new ForbiddenException("Faqat admin uchun"));
                    }
                    return Mono.empty();
                });
    }

    private Long principalId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
