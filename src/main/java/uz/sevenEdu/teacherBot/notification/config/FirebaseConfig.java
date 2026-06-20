package uz.sevenEdu.teacherBot.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;

/**
 * Firebase Admin SDK init.
 * Service account JSON yo'li berilmagan/topilmagan bo'lsa — push (FCM) o'chiq qoladi
 * (bean null bo'ladi, ilova bemalol ishlaydi).
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
                log.warn("Firebase service-account-path berilmagan — push (FCM) o'chiq.");
                return null;
            }
            File file = new File(serviceAccountPath);
            if (!file.exists()) {
                log.warn("Firebase service account fayli topilmadi: {} — push (FCM) o'chiq.", serviceAccountPath);
                return null;
            }
            FirebaseOptions options;
            try (FileInputStream in = new FileInputStream(file)) {
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(in))
                        .build();
            }
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("Firebase Admin SDK ishga tushdi — push (FCM) yoqildi.");
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            log.error("Firebase init xatosi — push (FCM) o'chiq: {}", e.getMessage());
            return null;
        }
    }
}
