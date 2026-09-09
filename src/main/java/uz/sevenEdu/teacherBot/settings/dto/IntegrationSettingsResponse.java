package uz.sevenEdu.teacherBot.settings.dto;

import java.time.LocalDateTime;

/**
 * Admin panel uchun xavfsiz ko'rinish. Token va parol hech qachon API orqali
 * qaytarilmaydi; faqat ularning sozlanganligi va maskasi ko'rsatiladi.
 */
public record IntegrationSettingsResponse(
        TelegramSettings telegram,
        EskizSettings eskiz,
        LocalDateTime updatedAt
) {
    public record TelegramSettings(
            String botUsername,
            boolean pollingEnabled,
            boolean tokenConfigured,
            String tokenMasked,
            String tokenSource
    ) {
    }

    public record EskizSettings(
            boolean emailConfigured,
            String emailMasked,
            boolean passwordConfigured,
            String passwordMasked,
            String from,
            String credentialsSource
    ) {
    }
}
