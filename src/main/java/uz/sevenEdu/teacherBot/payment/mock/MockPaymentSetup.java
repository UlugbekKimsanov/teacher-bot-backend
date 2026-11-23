package uz.sevenEdu.teacherBot.payment.mock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Lokal sinov muhitini tayyorlaydi (faqat `app.mock-payments.enabled=true`, ya'ni "mock" profili):
 *  - `payment_methods`ga "mock" usulini qo'shadi/yoqadi;
 *  - test o'quvchini yaratadi yoki parolini konfiguratsiyadagi qiymatga qaytaradi.
 * "prod" profili bilan birga yoqilsa ilova ishga TUSHMAYDI.
 */
@Component
@ConditionalOnProperty(name = "app.mock-payments.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class MockPaymentSetup implements ApplicationRunner {

    static final String METHOD_CODE = "mock";

    private final Environment environment;
    private final DatabaseClient databaseClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.mock-payments.test-user.email:}")
    private String testEmail;
    @Value("${app.mock-payments.test-user.password:}")
    private String testPassword;
    @Value("${app.mock-payments.test-user.first-name:Test}")
    private String testFirstName;
    @Value("${app.mock-payments.test-user.last-name:Oquvchi}")
    private String testLastName;
    @Value("${app.mock-payments.test-user.phone:}")
    private String testPhone;

    @Override
    public void run(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "app.mock-payments.enabled=true \"prod\" profili bilan birga ishlatilishi mumkin emas");
        }

        databaseClient.sql("INSERT INTO payment_methods (code, name, is_nasiya, enabled, order_index) "
                        + "VALUES ('" + METHOD_CODE + "', 'Mock to''lov (lokal sinov)', FALSE, TRUE, 0) "
                        + "ON CONFLICT (code) DO UPDATE SET enabled = TRUE")
                .fetch().rowsUpdated()
                .then(ensureTestUser())
                .block(Duration.ofSeconds(30));

        log.warn("MOCK TO'LOV MUHITI YOQILGAN — haqiqiy to'lov olinmaydi. Test o'quvchi: {} "
                + "(parol: application-mock.yml)", testEmail);
    }

    private Mono<Void> ensureTestUser() {
        if (testEmail.isBlank() || testPassword.isBlank()) return Mono.empty();
        String encoded = passwordEncoder.encode(testPassword);
        return userRepository.findByEmail(testEmail)
                .flatMap(user -> {
                    user.setPassword(encoded);
                    return userRepository.save(user);
                })
                .switchIfEmpty(Mono.defer(() -> userRepository.save(BaseUser.builder()
                        .firstName(testFirstName)
                        .lastName(testLastName)
                        .email(testEmail)
                        .phone(testPhone.isBlank() ? null : testPhone)
                        .password(encoded)
                        .role(UserRole.STUDENT)
                        .createdAt(LocalDateTime.now())
                        .build())))
                .then();
    }
}
