package uz.sevenEdu.teacherBot.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import java.time.Duration;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private static final String OTP_PREFIX = "otp:";
    private static final long OTP_TTL_MINUTES = 5;

    public Mono<Void> sendOtp(String email, boolean isLogin) {
        Mono<Boolean> check = isLogin
                ? userRepository.existsByEmail(email).flatMap(exists -> exists ? Mono.just(true) : Mono.error(new BadRequestException("Bu email ro'yxatdan o'tmagan!")))
                : userRepository.existsByEmail(email).flatMap(exists -> exists ? Mono.error(new BadRequestException("Bu email allaqachon ro'yxatdan o'tgan!")) : Mono.just(true));

        return check.flatMap(ok -> {
            String otpCode = generateOtp();
            return redisTemplate.opsForValue()
                    .set(OTP_PREFIX + email, otpCode, Duration.ofMinutes(OTP_TTL_MINUTES))
                    .then(Mono.fromCallable(() -> {
                        sendEmail(email, otpCode);
                        return true;
                    }).subscribeOn(Schedulers.boundedElastic()))
                    .then();
        });
    }

    public Mono<Void> verifyOtp(String email, String otpCode) {
        return redisTemplate.opsForValue().get(OTP_PREFIX + email)
                .switchIfEmpty(Mono.error(new BadRequestException("OTP kod topilmadi yoki muddati o'tgan")))
                .flatMap(storedOtp -> {
                    if (!storedOtp.equals(otpCode)) {
                        return Mono.error(new BadRequestException("OTP kod noto'g'ri"));
                    }
                    return redisTemplate.delete(OTP_PREFIX + email).then();
                });
    }

    private void sendEmail(String email, String otpCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("OAZIS - Tasdiqlash kodi");
            helper.setText(buildHtml(otpCode), true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new BadRequestException("Email yuborishda xatolik yuz berdi");
        }
    }

    private String generateOtp() {
        return String.format("%05d", new Random().nextInt(100000));
    }

    private String buildHtml(String otp) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#0F172A;font-family:Arial,sans-serif;">
                  <div style="max-width:480px;margin:40px auto;background:#1E293B;
                              border-radius:16px;padding:32px;border:1px solid #334155;">
                    <div style="display:flex;align-items:center;margin-bottom:24px;">
                      <div style="width:44px;height:44px;background:linear-gradient(135deg,#1800AD,#2D0AD4);
                                  border-radius:12px;display:flex;align-items:center;
                                  justify-content:center;margin-right:12px;">
                        <span style="color:white;font-size:22px;">🎓</span>
                      </div>
                      <div>
                        <div style="color:white;font-size:18px;font-weight:bold;">OAZIS</div>
                        <div style="color:#94A3B8;font-size:13px;">Academy</div>
                      </div>
                    </div>
                    <h2 style="color:white;margin:0 0 8px;">Emailingizni tasdiqlang</h2>
                    <p style="color:#94A3B8;margin:0 0 28px;">
                      Ro'yxatdan o'tishni yakunlash uchun quyidagi kodni kiriting:
                    </p>
                    <div style="background:#0F172A;border:2px solid #1800AD;border-radius:12px;
                                padding:20px;text-align:center;margin-bottom:24px;">
                      <span style="color:white;font-size:36px;font-weight:bold;
                                   letter-spacing:10px;">%s</span>
                    </div>
                    <p style="color:#64748B;font-size:13px;line-height:1.6;margin:0;">
                      Bu kod <strong style="color:#94A3B8;">5 daqiqa</strong> davomida amal qiladi.<br>
                      Agar siz bu so'rovni yubormagan bo'lsangiz, ushbu xatni e'tiborsiz qoldiring.
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(otp);
    }
}
