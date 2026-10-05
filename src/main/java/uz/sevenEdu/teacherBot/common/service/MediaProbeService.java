package uz.sevenEdu.teacherBot.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Media fayl metama'lumotlarini aniqlash — hozircha faqat davomiylik.
 *
 * <p>ffprobe (ffmpeg paketining bir qismi) tizimda bo'lsa ishlatiladi. Bo'lmasa
 * xato qaytarmaydi, shunchaki {@code null} beradi: bunday holatda brauzer
 * tomonida aniqlangan davomiylik kuchda qoladi. Ya'ni ffprobe — aniqlikni
 * oshiruvchi ixtiyoriy qatlam, majburiy bog'liqlik emas.
 *
 * <p>Server o'rnatish: {@code apt install ffmpeg} (ffprobe shu paketda keladi).
 */
@Service
public class MediaProbeService {

    private static final Logger log = LoggerFactory.getLogger(MediaProbeService.class);

    /** Katta fayllarda ham ffprobe faqat sarlavhani o'qiydi — 20s yetarlidan ortiq. */
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(20);

    private final String ffprobePath;

    public MediaProbeService(@Value("${app.media.ffprobe-path:ffprobe}") String ffprobePath) {
        this.ffprobePath = ffprobePath;
    }

    /**
     * Video/audio faylning davomiyligini sekundlarda qaytaradi.
     * Aniqlab bo'lmasa (ffprobe yo'q, fayl buzuq, timeout) — {@code null}.
     */
    public Mono<Integer> probeDurationSec(Path file) {
        if (file == null) return Mono.empty();
        return Mono.fromCallable(() -> runFfprobe(file))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.warn("ffprobe ishlamadi ({}): {}", file.getFileName(), e.getMessage());
                    return Mono.empty();
                })
                .flatMap(Mono::justOrEmpty);
    }

    private Integer runFfprobe(Path file) throws Exception {
        if (!Files.isReadable(file)) {
            log.warn("ffprobe uchun fayl o'qilmaydi: {}", file);
            return null;
        }
        Process process = new ProcessBuilder(List.of(
                ffprobePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                file.toString()
        )).redirectErrorStream(true).start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.readLine();
        }

        if (!process.waitFor(PROBE_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            log.warn("ffprobe timeout: {}", file.getFileName());
            return null;
        }
        if (process.exitValue() != 0 || output == null || output.isBlank()) {
            return null;
        }

        try {
            double seconds = Double.parseDouble(output.trim());
            if (!Double.isFinite(seconds) || seconds <= 0) return null;
            return (int) Math.round(seconds);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
