package uz.sevenEdu.teacherBot.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Media fayl metama'lumotlarini aniqlash — davomiylik va video ma'lumoti
 * (o'lcham, bitreyt, fps — tejamkor variant yaratish qarori uchun).
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

    /** To'liq JSON probe (oqimlar ro'yxati) — biroz ko'proq vaqt olishi mumkin. */
    private static final Duration INFO_TIMEOUT = Duration.ofSeconds(60);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String ffprobePath;

    /**
     * Video haqida asosiy ma'lumot (transkod qarorlari uchun).
     *
     * @param width          birinchi video oqim kengligi (px)
     * @param height         balandligi (px)
     * @param videoBitrate   video oqim bitreyti (bit/s), noma'lum bo'lsa null
     * @param overallBitrate umumiy bitreyt (bit/s), noma'lum bo'lsa null
     * @param fps            kadr chastotasi, noma'lum bo'lsa null
     * @param durationSec    davomiylik (s), noma'lum bo'lsa null
     * @param hasAudio       audio oqim bormi
     */
    public record VideoInfo(int width, int height, Long videoBitrate, Long overallBitrate,
                            Double fps, Double durationSec, boolean hasAudio) {
        public int shortSide() {
            return Math.min(width, height);
        }
    }

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

    /** {@link #probeVideoInfoBlocking} ning reaktiv varianti (boundedElastic'da). */
    public Mono<VideoInfo> probeVideoInfo(Path file) {
        if (file == null) return Mono.empty();
        return Mono.fromCallable(() -> java.util.Optional.ofNullable(probeVideoInfoBlocking(file)))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.empty())
                .flatMap(Mono::justOrEmpty);
    }

    /**
     * ffprobe JSON chiqishidan video ma'lumotini oladi. BLOKLOVCHI — faqat fon
     * (event-loop bo'lmagan) oqimidan chaqiring. Aniqlab bo'lmasa {@code null}.
     */
    public VideoInfo probeVideoInfoBlocking(Path file) {
        if (file == null || !Files.isReadable(file)) return null;
        Process process = null;
        try {
            process = new ProcessBuilder(List.of(
                    ffprobePath,
                    "-v", "error",
                    "-print_format", "json",
                    "-show_format",
                    "-show_streams",
                    file.toString()
            )).redirectError(ProcessBuilder.Redirect.DISCARD).start();

            // stdout'ni to'liq o'qiymiz (bufer to'lib jarayon qotib qolmasligi uchun)
            byte[] out = process.getInputStream().readAllBytes();
            if (!process.waitFor(INFO_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("ffprobe (info) timeout: {}", file.getFileName());
                return null;
            }
            if (process.exitValue() != 0 || out.length == 0) return null;
            return parseVideoInfo(JSON.readTree(out));
        } catch (Exception e) {
            log.warn("ffprobe (info) ishlamadi ({}): {}", file.getFileName(), e.getMessage());
            if (process != null) process.destroyForcibly();
            return null;
        }
    }

    static VideoInfo parseVideoInfo(JsonNode root) {
        JsonNode video = null;
        boolean hasAudio = false;
        for (JsonNode stream : root.path("streams")) {
            String type = stream.path("codec_type").asText("");
            // Muqova rasmi (attached_pic) haqiqiy video oqim emas
            boolean attachedPic = stream.path("disposition").path("attached_pic").asInt(0) == 1;
            if ("video".equals(type) && video == null && !attachedPic) video = stream;
            if ("audio".equals(type)) hasAudio = true;
        }
        if (video == null) return null;
        int width = video.path("width").asInt(0);
        int height = video.path("height").asInt(0);
        if (width <= 0 || height <= 0) return null;

        JsonNode format = root.path("format");
        Double duration = parsePositiveDouble(format.path("duration").asText(null));
        if (duration == null) duration = parsePositiveDouble(video.path("duration").asText(null));

        Long videoBitrate = parsePositiveLong(video.path("bit_rate").asText(null));
        Long overallBitrate = parsePositiveLong(format.path("bit_rate").asText(null));
        if (overallBitrate == null && duration != null) {
            Long size = parsePositiveLong(format.path("size").asText(null));
            if (size != null) overallBitrate = Math.round(size * 8 / duration);
        }

        Double fps = parseRate(video.path("avg_frame_rate").asText(null));
        if (fps == null) fps = parseRate(video.path("r_frame_rate").asText(null));

        return new VideoInfo(width, height, videoBitrate, overallBitrate, fps, duration, hasAudio);
    }

    /** "30000/1001" yoki "25" ko'rinishidagi kadr chastotasi. */
    private static Double parseRate(String rate) {
        if (rate == null || rate.isBlank()) return null;
        try {
            int slash = rate.indexOf('/');
            double value = slash < 0
                    ? Double.parseDouble(rate)
                    : Double.parseDouble(rate.substring(0, slash)) / Double.parseDouble(rate.substring(slash + 1));
            return Double.isFinite(value) && value > 0 && value < 1000 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parsePositiveDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            double d = Double.parseDouble(value);
            return Double.isFinite(d) && d > 0 ? d : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parsePositiveLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            long l = Long.parseLong(value.trim());
            return l > 0 ? l : null;
        } catch (NumberFormatException e) {
            return null;
        }
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
