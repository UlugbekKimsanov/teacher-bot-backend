package uz.sevenEdu.teacherBot.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.util.Map;

/**
 * Foydalanuvchi Telegram botni ulash/uzish uchun endpointlar.
 */
@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
public class TelegramLinkController {

    private final UserRepository userRepository;
    private final IntegrationSettingsService settingsService;

    /**
     * Telegram bot ulash uchun deeplink qaytaradi.
     * Deeplink: https://t.me/{botUsername}?start=link_{userId}
     */
    @GetMapping("/link")
    public Mono<ApiResponse<Map<String, Object>>> getLinkInfo(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return settingsService.resolveTelegram()
                .flatMap(settings -> userRepository.findById(userId).map(user -> {
                    boolean isLinked = user.getTelegramChatId() != null;
                    String username = settings.botUsername() != null ? settings.botUsername() : "";
                    String deeplink = username.isBlank()
                            ? ""
                            : "https://t.me/" + username + "?start=link_" + userId;
                    return ApiResponse.ok(Map.of(
                            "isLinked", isLinked,
                            "deeplink", deeplink,
                            "botUsername", username
                    ));
                }));
    }

    /**
     * Telegram botni uzish (unlink)
     */
    @DeleteMapping("/link")
    public Mono<ApiResponse<String>> unlinkTelegram(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setTelegramChatId(null);
                    return userRepository.save(user);
                })
                .thenReturn(ApiResponse.ok("Telegram bot uzildi", (String) null));
    }
}
