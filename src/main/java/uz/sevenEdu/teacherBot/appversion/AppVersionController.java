package uz.sevenEdu.teacherBot.appversion;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.LocalDateTime;

/**
 * Mobil ilova versiyasi:
 *  - GET  /api/v1/app-version — public; ilova kirishda o'z buildini solishtiradi;
 *  - PUT  /api/v1/admin/app-version — admin yangi build chiqqanda qiymatni oshiradi
 *    (shu ondan boshlab eski ilovalarda yangilanish oynasi chiqa boshlaydi).
 */
@RestController
@RequiredArgsConstructor
public class AppVersionController {

    private final AppVersionRepository repository;
    private final UserRepository userRepository;

    @GetMapping("/api/v1/app-version")
    public Mono<ApiResponse<AppVersion>> get() {
        return repository.findById(1).map(ApiResponse::ok);
    }

    @PutMapping("/api/v1/admin/app-version")
    public Mono<ApiResponse<AppVersion>> update(Authentication auth,
                                                @RequestBody UpdateRequest request) {
        return requireAdmin(auth).then(Mono.defer(() -> {
            if (request.getLatestBuild() == null
                    || request.getLatestBuild() < 1 || request.getLatestBuild() > 1_000_000) {
                return Mono.error(new BadRequestException(
                        "Build raqami 1 dan 1 000 000 gacha bo'lishi kerak"));
            }
            if (request.getStoreUrl() != null && !request.getStoreUrl().isBlank()
                    && !request.getStoreUrl().startsWith("https://")) {
                return Mono.error(new BadRequestException(
                        "Do'kon havolasi https:// bilan boshlanishi kerak"));
            }
            return repository.findById(1)
                    .flatMap(v -> {
                        v.setLatestBuild(request.getLatestBuild());
                        if (request.getLatestVersion() != null) {
                            v.setLatestVersion(trim(request.getLatestVersion(), 20));
                        }
                        if (request.getMessage() != null) {
                            v.setMessage(trim(request.getMessage(), 1000));
                        }
                        if (request.getStoreUrl() != null && !request.getStoreUrl().isBlank()) {
                            v.setStoreUrl(trim(request.getStoreUrl(), 500));
                        }
                        v.setUpdatedAt(LocalDateTime.now());
                        return repository.save(v);
                    })
                    .map(saved -> ApiResponse.ok("Saqlandi", saved));
        }));
    }

    private static String trim(String value, int max) {
        String t = value.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private Mono<Void> requireAdmin(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            return Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi"));
        }
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi")))
                .flatMap(user -> user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.HEAD_ADMIN
                        ? Mono.<Void>error(new ForbiddenException("Faqat admin uchun"))
                        : Mono.<Void>empty());
    }

    @Data
    public static class UpdateRequest {
        private Integer latestBuild;
        private String latestVersion;
        private String message;
        private String storeUrl;
    }
}
