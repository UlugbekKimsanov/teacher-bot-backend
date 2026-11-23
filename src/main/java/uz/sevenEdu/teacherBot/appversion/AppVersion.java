package uz.sevenEdu.teacherBot.appversion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Mobil ilovaning so'nggi versiyasi (bitta qator, id=1). Ilova kirishda
 * o'z build raqamini shu bilan solishtiradi — eskirgan bo'lsa ixtiyoriy
 * yangilanish oynasi chiqadi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_version")
public class AppVersion {
    @Id
    private Integer id;
    /** pubspec'dagi build raqami (masalan 1.0.0+15 dagi 15). */
    private Integer latestBuild;
    /** Ko'rsatish uchun versiya nomi (masalan "1.0.1"). */
    private String latestVersion;
    /** Yangilanish oynasidagi matn. */
    private String message;
    /** Do'kon havolasi (Google Play). */
    private String storeUrl;
    private LocalDateTime updatedAt;
}
