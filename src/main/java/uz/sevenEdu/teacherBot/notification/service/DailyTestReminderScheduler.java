package uz.sevenEdu.teacherBot.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

/**
 * Har kuni soat 09:00 da (Toshkent vaqti) barcha o'quvchilarga "ilovaga kiring"
 * eslatmasi — ilova ichidagi xabar + push (FCM) birga ketadi.
 *
 * Matn va yoqish/o'chirish application.yml dagi `app.daily-reminder.*`
 * qiymatlari bilan boshqariladi (standart: yoqilgan). Mehmon hisobiga yuborilmaydi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyTestReminderScheduler {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.daily-reminder.enabled:true}")
    private boolean enabled;

    @Value("${app.daily-reminder.title:OAZIS sizni kutmoqda! 🎓}")
    private String title;

    @Value("${app.daily-reminder.body:Ilovaga kirib, darslar va yangi imkoniyatlarni sinab ko'ring. Fikringiz biz uchun muhim!}")
    private String body;

    /** Server UTC'da ishlaydi — zone aniq ko'rsatilgan: 09:00 Toshkent. */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Tashkent")
    public void sendDailyReminder() {
        if (!enabled) return;
        userRepository.findByRole("STUDENT")
                .filter(u -> !Boolean.TRUE.equals(u.getIsGuest()))
                // 16 talik parallellik — DB pool va FCM'ni bosib qo'ymaslik uchun
                .flatMap(u -> notificationService
                        .send(u.getId(), title, body, "DAILY_REMINDER", null)
                        .onErrorResume(e -> Mono.empty()), 16)
                .count()
                .doOnNext(c -> log.info("Kunlik eslatma yuborildi: {} ta o'quvchi", c))
                .doOnError(e -> log.error("Kunlik eslatma xatosi", e))
                .subscribe();
    }
}
