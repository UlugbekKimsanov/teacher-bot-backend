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
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import uz.sevenEdu.teacherBot.telegram.entity.TelegramSubscriber;
import uz.sevenEdu.teacherBot.telegram.repository.TelegramSubscriberRepository;
import uz.sevenEdu.teacherBot.settings.service.IntegrationSettingsService;

/**
 * Telegram Bot API orqali xabar yuborish servisi.
 * WebClient ishlatadi (WebFlux reactive).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final IntegrationSettingsService settingsService;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;
    private final TelegramSubscriberRepository subscriberRepository;

    private static final String TELEGRAM_API = "https://api.telegram.org";

    /**
     * Bitta chatId ga xabar yuborish
     */
    public Mono<Void> sendMessage(Long chatId, String text) {
        if (chatId == null) return Mono.empty();

        return settingsService.resolveTelegram()
                .flatMap(settings -> {
                    if (settings.botToken() == null || settings.botToken().isBlank()) {
                        return Mono.empty();
                    }

                    String url = TELEGRAM_API + "/bot" + settings.botToken() + "/sendMessage";
                    return webClientBuilder.clone()
                            // Tashqi xizmat osilsa zanjir bloklanmasligi uchun timeout.
                            .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                                    reactor.netty.http.client.HttpClient.create()
                                            .responseTimeout(java.time.Duration.ofSeconds(10))))
                            .build()
                            .post()
                            .uri(url)
                            .bodyValue(Map.of(
                                    "chat_id", chatId,
                                    "text", text,
                                    "parse_mode", "HTML"
                            ))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .doOnError(e -> log.warn("Telegram xabar yuborishda xato (chatId={}): {}",
                                    chatId, e.getClass().getSimpleName()))
                            .onErrorResume(e -> Mono.empty())
                            .then();
                });
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

    /** Botni /start qilgan barcha faol obunachilarga xabar yuboradi. */
    public Mono<Void> notifySubscribers(String text) {
        return Flux.concat(
                        subscriberRepository.findByActiveTrue().map(TelegramSubscriber::getChatId),
                        userRepository.findAll()
                                .filter(user -> user.getTelegramChatId() != null)
                                .map(BaseUser::getTelegramChatId)
                )
                .distinct()
                .flatMap(chatId -> sendMessage(chatId, text), 4)
                .then();
    }

    public Mono<Void> handleUpdate(JsonNode update) {
        JsonNode message = update.path("message");
        JsonNode chat = message.path("chat");
        if (message.isMissingNode() || chat.isMissingNode() || !chat.has("id")) return Mono.empty();

        long chatId = chat.path("id").asLong();
        String username = textOrNull(message.path("from").path("username"));
        String firstName = textOrNull(message.path("from").path("first_name"));
        String lastName = textOrNull(message.path("from").path("last_name"));
        String fullName = String.join(" ",
                firstName == null ? "" : firstName,
                lastName == null ? "" : lastName).trim();
        String text = textOrNull(message.path("text"));

        if ("/stop".equalsIgnoreCase(text)) {
            return setSubscriberActive(chatId, false)
                    .then(sendMessage(chatId, "Xabarlar o'chirildi. Qayta ulanish uchun /start yuboring."));
        }

        return upsertSubscriber(chatId, username, fullName)
                .then(text != null && text.startsWith("/start")
                        ? handleStartCommand(chatId, text)
                        : Mono.just("OAZIS Bot\nYangi arizalar shu yerga avtomatik yuboriladi.\nXabarlarni o'chirish: /stop"))
                .flatMap(reply -> sendMessage(chatId, reply));
    }

    private Mono<TelegramSubscriber> upsertSubscriber(Long chatId, String username, String fullName) {
        LocalDateTime now = LocalDateTime.now();
        return subscriberRepository.findById(chatId)
                .defaultIfEmpty(TelegramSubscriber.builder()
                        .chatId(chatId)
                        .subscribedAt(now)
                        .build())
                .flatMap(subscriber -> {
                    subscriber.setUsername(username);
                    subscriber.setFullName(fullName == null || fullName.isBlank() ? null : fullName);
                    subscriber.setActive(true);
                    subscriber.setUpdatedAt(now);
                    return subscriberRepository.save(subscriber);
                });
    }

    private Mono<Void> setSubscriberActive(Long chatId, boolean active) {
        return subscriberRepository.findById(chatId)
                .flatMap(subscriber -> {
                    subscriber.setActive(active);
                    subscriber.setUpdatedAt(LocalDateTime.now());
                    return subscriberRepository.save(subscriber);
                })
                .then();
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
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
