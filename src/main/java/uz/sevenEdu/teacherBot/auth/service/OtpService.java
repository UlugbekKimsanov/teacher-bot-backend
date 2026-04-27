package uz.sevenEdu.teacherBot.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import uz.sevenEdu.teacherBot.auth.repository.UserRepository;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    private static final String OTP_PREFIX = "otp:";
    private static final long OTP_TTL_MINUTES = 5;

    public void sendOtp(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Bu email allaqachon ro'yxatdan o'tgan!");
        }

        String otpCode = generateOtp();
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otpCode, OTP_TTL_MINUTES, TimeUnit.MINUTES);

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

    public void verifyOtp(String email, String otpCode) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + email);
        if (storedOtp == null) {
            throw new BadRequestException("OTP kod topilmadi yoki muddati o'tgan");
        }
        if (!storedOtp.equals(otpCode)) {
            throw new BadRequestException("OTP kod noto'g'ri");
        }
        redisTemplate.delete(OTP_PREFIX + email);
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
