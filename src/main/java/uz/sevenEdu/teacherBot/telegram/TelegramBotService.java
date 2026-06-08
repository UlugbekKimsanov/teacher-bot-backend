package uz.sevenEdu.teacherBot.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.util.Map;

/**
 * Telegram Bot API orqali xabar yuborish servisi.
 * WebClient ishlatadi (WebFlux reactive).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramProperties properties;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;

    private static final String TELEGRAM_API = "https://api.telegram.org";

    /**
     * Bitta chatId ga xabar yuborish
     */
    public Mono<Void> sendMessage(Long chatId, String text) {
        if (chatId == null || properties.getBotToken() == null || properties.getBotToken().isBlank()) {
            return Mono.empty();
        }

        String url = TELEGRAM_API + "/bot" + properties.getBotToken() + "/sendMessage";

        return webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(Map.of(
                        "chat_id", chatId,
                        "text", text,
                        "parse_mode", "HTML"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.warn("Telegram xabar yuborishda xato (chatId={}): {}", chatId, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    /**
     * Telegram bot ulangan barcha admin larga xabar yuborish
     */
    public Mono<Void> notifyAdmins(String text) {
        return userRepository.findByRole(UserRole.ADMIN.name())
                .filter(user -> user.getTelegramChatId() != null)
                .concatMap(admin -> sendMessage(admin.getTelegramChatId(), text))
                .then();
    }

    /**
     * Telegram bot ulangan barcha foydalanuvchilarga (admin + teacher) xabar yuborish
     */
    public Mono<Void> notifyAdminsAndTeachers(String text) {
        return Flux.merge(
                userRepository.findByRole(UserRole.ADMIN.name()),
                userRepository.findByRole(UserRole.TEACHER.name())
        )
                .filter(user -> user.getTelegramChatId() != null)
                .concatMap(user -> sendMessage(user.getTelegramChatId(), text))
                .then();
    }

    /**
     * /start deeplink dan userId ni parse qilib, user ga telegram chatId saqlash.
     * Deeplink formati: /start link_{userId}
     */
    public Mono<String> handleStartCommand(Long chatId, String text) {
        if (text == null || !text.startsWith("/start")) {
            return Mono.just("Noma'lum buyruq. /start link_{userId} formatida yuboring.");
        }

        String[] parts = text.trim().split("\\s+");
        if (parts.length < 2 || !parts[1].startsWith("link_")) {
            return Mono.just("Salom! OAZIS ilovasidan \"Telegram ulash\" tugmasini bosing.");
        }

        try {
            Long userId = Long.parseLong(parts[1].substring(5)); // "link_123" → 123
            return userRepository.findById(userId)
                    .flatMap(user -> {
                        user.setTelegramChatId(chatId);
                        return userRepository.save(user);
                    })
                    .map(user -> "Muvaffaqiyatli ulandi! " + user.getFirstName()
                            + ", endi sotuv xabarlari shu yerga keladi.")
                    .defaultIfEmpty("Foydalanuvchi topilmadi.");
        } catch (NumberFormatException e) {
            return Mono.just("Noto'g'ri havola. Iltimos, ilovadan qayta urinib ko'ring.");
        }
    }
}
