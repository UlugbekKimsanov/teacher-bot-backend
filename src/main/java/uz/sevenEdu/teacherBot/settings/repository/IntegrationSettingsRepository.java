package uz.sevenEdu.teacherBot.settings.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.settings.entity.IntegrationSettings;

import java.time.LocalDateTime;

public interface IntegrationSettingsRepository extends ReactiveCrudRepository<IntegrationSettings, Long> {

    @Query("""
            INSERT INTO integration_settings (
                id,
                telegram_bot_token,
                telegram_bot_username,
                telegram_polling_enabled,
                eskiz_email,
                eskiz_password,
                eskiz_from,
                updated_at,
                updated_by
            ) VALUES (
                1,
                :telegramBotToken,
                :telegramBotUsername,
                :telegramPollingEnabled,
                :eskizEmail,
                :eskizPassword,
                :eskizFrom,
                :updatedAt,
                :updatedBy
            )
            ON CONFLICT (id) DO UPDATE SET
                telegram_bot_token = EXCLUDED.telegram_bot_token,
                telegram_bot_username = EXCLUDED.telegram_bot_username,
                telegram_polling_enabled = EXCLUDED.telegram_polling_enabled,
                eskiz_email = EXCLUDED.eskiz_email,
                eskiz_password = EXCLUDED.eskiz_password,
                eskiz_from = EXCLUDED.eskiz_from,
                updated_at = EXCLUDED.updated_at,
                updated_by = EXCLUDED.updated_by
            RETURNING id,
                      telegram_bot_token,
                      telegram_bot_username,
                      telegram_polling_enabled,
                      eskiz_email,
                      eskiz_password,
                      eskiz_from,
                      updated_at,
                      updated_by
            """)
    Mono<IntegrationSettings> upsert(
            String telegramBotToken,
            String telegramBotUsername,
            Boolean telegramPollingEnabled,
            String eskizEmail,
            String eskizPassword,
            String eskizFrom,
            LocalDateTime updatedAt,
            Long updatedBy
    );
}
