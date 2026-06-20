package uz.sevenEdu.teacherBot.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.notification.entity.Notification;
import uz.sevenEdu.teacherBot.notification.repository.NotificationRepository;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Avtomatik "bugun darslarni o'tkazib yubormang" eslatmasi.
 *
 * Har kuni 18:00 da: kursga yozilgan, lekin bugun ilovaga kirmagan o'quvchilarga
 * eslatma yuboriladi. Agar o'quvchi eslatmani o'qimasa:
 *   1-eslatma -> 3 kundan keyin -> 2-eslatma -> 7 kundan keyin -> 3-eslatma -> to'xtaydi.
 * Eslatma o'qilsa, eskalatsiya to'xtaydi (keyinroq yana nofaol bo'lsa, yangi tsikl boshlanadi).
 *
 * Eskalatsiya bosqichi notification.refId da saqlanadi (1, 2, 3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InactivityReminderScheduler {

    private static final int MAX_STAGE = 3;       // jami 3 marta yuboriladi
    private static final long GAP_AFTER_FIRST = 3; // 1->2: 3 kun
    private static final long GAP_AFTER_SECOND = 7; // 2->3: 7 kun

    private final UserRepository userRepository;
    private final UserCourseRepository userCourseRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /** Har kuni soat 18:00 (server vaqti bo'yicha). */
    @Scheduled(cron = "0 0 18 * * *")
    public void sendInactivityReminders() {
        userRepository.findByRole("STUDENT")
                // faqat kamida bitta kursga yozilgan o'quvchilar
                .filterWhen(u -> userCourseRepository.findByUserId(u.getId()).hasElements())
                .flatMap(this::processStudent)
                .doOnError(e -> log.error("Inactivity reminder xatosi", e))
                .subscribe();
    }

    private Mono<Void> processStudent(BaseUser u) {
        boolean activeToday = u.getLastActiveAt() != null
                && u.getLastActiveAt().toLocalDate().isEqual(LocalDate.now());

        return notificationRepository.findLatestInactivity(u.getId())
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(opt -> {
                    // Hech qachon eslatma yuborilmagan YOKI oxirgisi o'qilgan -> yangi tsikl
                    if (opt.isEmpty() || Boolean.TRUE.equals(opt.get().getIsRead())) {
                        return activeToday ? Mono.empty() : send(u, 1);
                    }
                    // O'qilmagan eslatma bor -> eskalatsiya
                    Notification latest = opt.get();
                    int stage = latest.getRefId() != null ? latest.getRefId().intValue() : 1;
                    if (stage >= MAX_STAGE) {
                        return Mono.empty(); // 3 marta yuborilgan, to'xtaymiz
                    }
                    long gapDays = (stage == 1) ? GAP_AFTER_FIRST : GAP_AFTER_SECOND;
                    LocalDateTime due = latest.getCreatedAt() != null
                            ? latest.getCreatedAt().plusDays(gapDays)
                            : LocalDateTime.now();
                    if (LocalDateTime.now().isBefore(due)) {
                        return Mono.empty(); // hali vaqti kelmagan
                    }
                    return send(u, stage + 1);
                })
                .then();
    }

    private Mono<Void> send(BaseUser u, int stage) {
        // NotificationService orqali — ilova ichidagi xabar + push (FCM) birga ketadi
        return notificationService.send(
                u.getId(),
                "Bugun darslarni o'tkazib yubormang",
                "Bugun hali dars qilmadingiz. Keling, bugungi darslarni yakunlaymiz! 📚",
                "INACTIVITY",
                (long) stage // eskalatsiya bosqichi
        ).then();
    }
}
