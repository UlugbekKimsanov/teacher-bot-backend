package uz.sevenEdu.teacherBot.lesson.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;
import uz.sevenEdu.teacherBot.common.service.MediaProbeService;
import uz.sevenEdu.teacherBot.common.service.MediaProbeService.VideoInfo;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;
import uz.sevenEdu.teacherBot.lesson.repository.LessonRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dars videolari uchun fon ishlovi (ffmpeg):
 * <ol>
 *   <li><b>Faststart</b> — MP4/MOV asl faylda {@code moov} atomi boshiga ko'chiriladi
 *       (qayta kodlashsiz, {@code -c copy}), shunda pleer butun faylni yuklamasdan
 *       darhol o'ynay boshlaydi. Natija yangi nom bilan yoziladi va {@code video_url} almashtiriladi.</li>
 *   <li><b>Tejamkor variant</b> — ~480p, H.264 + AAC, past bitreyt
 *       ({@code lessons/{id}/video_low_{id}_{uid}.mp4}). Asl video allaqachon tejamkor bo'lsa
 *       yoki natija sezilarli kichik chiqmasa — SKIPPED.</li>
 * </ol>
 *
 * <p>Ishlar QAT'IY bittadan, alohida bitta fon oqimida bajariladi (kichik VPS — CPU/RAM'ni
 * Netty event-loop va boshqa so'rovlardan tortib olmaslik uchun). {@link #enqueue} bloklamaydi.
 *
 * <p>Ilova ishga tushganda va har ~10 daqiqada "sweep" holati NULL/PENDING/PROCESSING bo'lgan
 * lokal videolarni navbatga qo'yadi — shu tariqa mavjud eski videolar ham avtomatik ishlanadi
 * (backfill) va restartda uzilib qolgan ishlar qayta boshlanadi.
 *
 * <p>Hech qachon ilovani yiqitmaydi: har qanday xato — FAILED + qisqa xato matni.
 */
@Service
public class VideoTranscodeService {

    private static final Logger log = LoggerFactory.getLogger(VideoTranscodeService.class);

    /** ffmpeg o'rnatilmaganda yoziladigan xato matni (sweep shu bo'yicha qayta urinadi). */
    public static final String FFMPEG_MISSING = "ffmpeg topilmadi";

    private static final Set<String> FASTSTART_EXTENSIONS = Set.of(".mp4", ".m4v", ".mov");
    private static final Duration DB_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration MIN_TRANSCODE_TIMEOUT = Duration.ofMinutes(10);
    /** Davomiylik noma'lum bo'lsa transkod uchun limit. */
    private static final Duration UNKNOWN_DURATION_TIMEOUT = Duration.ofMinutes(90);
    private static final int ERROR_MAX_CHARS = 500;
    /** Tejamkor variant asl faylning shu ulushidan katta chiqsa — foydasiz, SKIPPED. */
    private static final double MIN_SAVING_RATIO = 0.85;
    /** Shu o'lcham va bitreytdan past asl video allaqachon tejamkor hisoblanadi. */
    private static final int ECONOMY_SHORT_SIDE = 480;
    private static final long ECONOMY_MAX_BITRATE = 1_000_000L;

    private final LessonRepository lessonRepository;
    private final FileStorageService fileStorageService;
    private final MediaProbeService mediaProbeService;
    private final String ffmpegPath;
    private final boolean enabled;

    /** Bitta fon oqimi — ishlar ketma-ket, hech qachon parallel emas. */
    private final ExecutorService executor;
    /** Navbatda kutayotgan dars ID'lari (takroriy qo'yishning oldini olish). */
    private final Set<Long> queued = ConcurrentHashMap.newKeySet();
    /** Hozir ishlanayotgan dars (sweep uni qayta navbatga qo'ymasligi uchun). */
    private volatile Long running;
    private volatile Process currentProcess;
    private volatile boolean shuttingDown;
    private final AtomicBoolean ffmpegMissingLogged = new AtomicBoolean(false);

    public VideoTranscodeService(LessonRepository lessonRepository,
                                 FileStorageService fileStorageService,
                                 MediaProbeService mediaProbeService,
                                 @Value("${app.media.ffmpeg-path:ffmpeg}") String ffmpegPath,
                                 @Value("${app.media.transcode.enabled:true}") boolean enabled) {
        this.lessonRepository = lessonRepository;
        this.fileStorageService = fileStorageService;
        this.mediaProbeService = mediaProbeService;
        this.ffmpegPath = ffmpegPath;
        this.enabled = enabled;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "video-transcode");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    // ── Navbat ─────────────────────────────────────────────

    /**
     * Darsni navbatga qo'yadi (bloklamaydi). Allaqachon navbatda bo'lsa — e'tiborsiz.
     * Hozir shu dars ishlanayotgan bo'lsa ham qo'yiladi (admin yangi video yuklagan bo'lishi mumkin;
     * eski ish natijasi poyga himoyasi tufayli saqlanmaydi).
     */
    public void enqueue(Long lessonId) {
        if (!enabled || lessonId == null || shuttingDown) return;
        if (!queued.add(lessonId)) return;
        try {
            executor.execute(() -> {
                queued.remove(lessonId);
                running = lessonId;
                try {
                    process(lessonId);
                } catch (Throwable e) {
                    log.error("Video ishlovi kutilmagan xato (lesson {}): {}", lessonId, e.toString());
                } finally {
                    running = null;
                }
            });
        } catch (RejectedExecutionException e) {
            queued.remove(lessonId);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // ffmpeg tekshiruvi (tashqi jarayon) startup oqimini ushlab turmasin
        if (!enabled) return;
        try {
            executor.execute(this::sweep);
        } catch (RejectedExecutionException ignored) {
        }
    }

    /** Har 10 daqiqada: ishlanmagan / uzilib qolgan videolarni navbatga qo'yish (backfill). */
    @Scheduled(initialDelay = 10 * 60 * 1000L, fixedDelay = 10 * 60 * 1000L)
    public void sweep() {
        if (!enabled || shuttingDown) return;
        // ffmpeg yo'q bo'lsa bazaga tegmaymiz — keyinroq o'rnatilganda o'zi ishlab ketadi.
        if (!ffmpegAvailable()) return;
        lessonRepository.findIdsNeedingVideoVariant()
                .concatWith(lessonRepository.findIdsFailedWithError(FFMPEG_MISSING))
                .filter(id -> !id.equals(running))
                .collectList()
                .subscribe(ids -> {
                    if (!ids.isEmpty()) log.info("Video ishlovi navbatiga {} ta dars qo'yildi", ids.size());
                    ids.forEach(this::enqueue);
                }, e -> log.warn("Video sweep xatosi: {}", e.getMessage()));
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        executor.shutdownNow();
        Process p = currentProcess;
        if (p != null) p.destroyForcibly();
    }

    // ── Ish (fon oqimida, bloklovchi) ──────────────────────

    private void process(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).block(DB_TIMEOUT);
        if (lesson == null) return;
        String src = lesson.getVideoUrl();
        String status = lesson.getVideoLowStatus();
        boolean retryMissingFfmpeg = Lesson.LOW_FAILED.equals(status)
                && lesson.getVideoLowError() != null && lesson.getVideoLowError().startsWith(FFMPEG_MISSING);
        boolean pending = status == null || Lesson.LOW_PENDING.equals(status)
                || Lesson.LOW_PROCESSING.equals(status) || retryMissingFfmpeg;
        if (!Lesson.isLocalVideo(src) || !pending) return;

        if (!ffmpegAvailable()) {
            setStatus(lessonId, src, Lesson.LOW_FAILED, FFMPEG_MISSING);
            return;
        }
        String oldLow = lesson.getVideoUrlLow();
        setStatus(lessonId, src, Lesson.LOW_PROCESSING, null);

        Path source = fileStorageService.resolveStoredPath(src);
        if (source == null || !Files.isRegularFile(source)) {
            setStatus(lessonId, src, Lesson.LOW_FAILED, "Asl video fayl topilmadi");
            return;
        }

        try {
            // Oldingi (restart/kill bilan uzilgan) ishdan qolgan .part fayllar — ishlar bittadan
            // bajarilgani uchun hozir bu papkada boshqa tirik .part yo'q.
            deleteStaleParts(source.getParent());

            // a) Faststart (asl fayl, qayta kodlashsiz)
            String fastStarted = faststart(lessonId, src, source);
            if (fastStarted == null) return; // video ish davomida almashtirilgan
            if (!fastStarted.equals(src)) {
                src = fastStarted;
                source = fileStorageService.resolveStoredPath(src);
            }

            // b) Tejamkor variant kerakmi?
            VideoInfo info = mediaProbeService.probeVideoInfoBlocking(source);
            if (info != null && info.shortSide() <= ECONOMY_SHORT_SIDE
                    && info.overallBitrate() != null && info.overallBitrate() <= ECONOMY_MAX_BITRATE) {
                log.info("Lesson {}: asl video allaqachon tejamkor ({}x{}, {} kbps) — SKIPPED",
                        lessonId, info.width(), info.height(), info.overallBitrate() / 1000);
                finish(lessonId, src, null, Lesson.LOW_SKIPPED, oldLow);
                return;
            }

            // c) Transkod
            long sourceSize = Files.size(source);
            Path dir = source.getParent();
            ensureFreeSpace(dir, sourceSize);
            String uid = newUid();
            Path out = dir.resolve("video_low_" + lessonId + "_" + uid + ".mp4");
            Path part = dir.resolve(out.getFileName() + ".part");
            Duration timeout = transcodeTimeout(info);
            try {
                runFfmpeg(transcodeArgs(source, part, info), timeout);
                moveAtomic(part, out);
            } finally {
                Files.deleteIfExists(part);
            }

            // d) Foydasi bormi?
            long outSize = Files.size(out);
            if (outSize >= sourceSize * MIN_SAVING_RATIO) {
                Files.deleteIfExists(out);
                log.info("Lesson {}: tejamkor variant yetarli kichik emas ({} / {} bayt) — SKIPPED",
                        lessonId, outSize, sourceSize);
                finish(lessonId, src, null, Lesson.LOW_SKIPPED, oldLow);
                return;
            }

            // e) Saqlash (video o'zgarmagan bo'lsa)
            String lowPath = fileStorageService.toStoredPath(out);
            if (!finish(lessonId, src, lowPath, Lesson.LOW_READY, oldLow)) {
                Files.deleteIfExists(out);
                return;
            }
            log.info("Lesson {}: tejamkor video tayyor ({} MB -> {} MB)", lessonId,
                    sourceSize / (1024 * 1024), outSize / (1024 * 1024));
        } catch (FfmpegMissingException e) {
            setStatus(lessonId, src, Lesson.LOW_FAILED, FFMPEG_MISSING);
        } catch (Exception e) {
            if (shuttingDown) return; // restartdan keyin PROCESSING holatidan qayta boshlanadi
            log.warn("Lesson {}: video ishlovi xatosi: {}", lessonId, e.getMessage());
            setStatus(lessonId, src, Lesson.LOW_FAILED, truncate(e.getMessage()));
        }
    }

    /**
     * MP4/MOV faylni faststart qiladi. Natija — amaldagi video_url (yangi yoki o'sha),
     * yoki null — agar video ish davomida boshqasiga almashtirilgan bo'lsa (ishni to'xtatish kerak).
     * Remux muvaffaqiyatsiz bo'lsa asl fayl saqlanadi va ish davom etadi.
     */
    private String faststart(Long lessonId, String src, Path source) throws IOException {
        String ext = extension(source.getFileName().toString());
        if (!FASTSTART_EXTENSIONS.contains(ext)) return src;
        if (isAlreadyFaststart(source)) return src;

        Path dir = source.getParent();
        ensureFreeSpace(dir, Files.size(source));
        Path out = dir.resolve("video_" + lessonId + "_" + newUid() + ".mp4");
        Path part = dir.resolve(out.getFileName() + ".part");
        try {
            runFfmpeg(List.of(
                    "-i", source.toString(),
                    // Faqat video/audio: iPhone MOV'dagi metadata (mebx) oqimlari MP4'ga sig'maydi
                    "-map", "0:v", "-map", "0:a?",
                    "-c", "copy",
                    "-movflags", "+faststart",
                    "-f", "mp4",
                    part.toString()), MIN_TRANSCODE_TIMEOUT.plusMinutes(20));
            moveAtomic(part, out);
        } catch (FfmpegMissingException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Lesson {}: faststart bajarilmadi, asl fayl qoldi: {}", lessonId, e.getMessage());
            Files.deleteIfExists(part);
            Files.deleteIfExists(out);
            return src;
        } finally {
            Files.deleteIfExists(part);
        }

        String newPath = fileStorageService.toStoredPath(out);
        Integer updated = lessonRepository.replaceVideoUrl(lessonId, src, newPath).block(DB_TIMEOUT);
        if (updated == null || updated == 0) {
            Files.deleteIfExists(out);
            return null;
        }
        deleteIfUnreferenced(src);
        log.info("Lesson {}: video faststart qilindi -> {}", lessonId, newPath);
        return newPath;
    }

    /**
     * Fayl boshidagi top-level atomlarni o'qiydi: {@code moov} {@code mdat}'dan oldin kelsa —
     * fayl allaqachon faststart. Faqat sarlavhalar o'qiladi (bir necha seek).
     */
    static boolean isAlreadyFaststart(Path file) {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long length = raf.length();
            long pos = 0;
            byte[] header = new byte[16];
            for (int i = 0; i < 64 && pos + 8 <= length; i++) {
                raf.seek(pos);
                raf.readFully(header, 0, 8);
                long size = ((header[0] & 0xFFL) << 24) | ((header[1] & 0xFFL) << 16)
                        | ((header[2] & 0xFFL) << 8) | (header[3] & 0xFFL);
                String type = new String(header, 4, 4, StandardCharsets.ISO_8859_1);
                if ("moov".equals(type)) return true;
                if ("mdat".equals(type)) return false;
                if (size == 1) { // 64-bitli hajm
                    if (pos + 16 > length) return false;
                    raf.readFully(header, 8, 8);
                    size = 0;
                    for (int b = 8; b < 16; b++) size = (size << 8) | (header[b] & 0xFFL);
                } else if (size == 0) {
                    return false; // oxirigacha cho'zilgan atom
                }
                if (size < 8) return false;
                pos += size;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    /** ffmpeg argumentlari — tejamkor variant (ffmpeg yo'li va umumiy flaglar runFfmpeg'da qo'shiladi). */
    static List<String> transcodeArgs(Path source, Path out, VideoInfo info) {
        int targetShort = info == null ? ECONOMY_SHORT_SIDE : Math.min(info.shortSide(), ECONOMY_SHORT_SIDE);
        targetShort = Math.max(2, targetShort - (targetShort % 2));
        int baseK = targetShort <= 360 ? 450 : 750;
        int maxrateK = baseK;
        Long srcVideoBps = info == null ? null
                : (info.videoBitrate() != null ? info.videoBitrate() : info.overallBitrate());
        if (srcVideoBps != null) {
            long capK = Math.max(250, Math.round(srcVideoBps * 0.6 / 1000));
            maxrateK = (int) Math.min(baseK, capK);
        }

        // Qisqa tomon = targetShort, nisbat saqlanadi, ikkinchi tomon juft (-2).
        // Tirnoqlar filtergraph ichida vergullarni himoya qiladi (shell ishlatilmaydi).
        String vf = "scale=w='if(gt(iw,ih),-2," + targetShort + ")':h='if(gt(iw,ih)," + targetShort + ",-2)'";
        if (info != null && info.fps() != null && info.fps() > 30.5) vf += ",fps=30";

        return List.of(
                "-i", source.toString(),
                "-map", "0:v:0", "-map", "0:a:0?",
                "-vf", vf,
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "27",
                "-maxrate", maxrateK + "k", "-bufsize", (maxrateK * 2) + "k",
                "-profile:v", "main", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "64k", "-ac", "2",
                "-movflags", "+faststart",
                "-threads", "2",
                "-f", "mp4",
                out.toString());
    }

    private static Duration transcodeTimeout(VideoInfo info) {
        if (info == null || info.durationSec() == null) return UNKNOWN_DURATION_TIMEOUT;
        Duration byDuration = Duration.ofSeconds((long) Math.ceil(info.durationSec() * 4));
        return byDuration.compareTo(MIN_TRANSCODE_TIMEOUT) > 0 ? byDuration : MIN_TRANSCODE_TIMEOUT;
    }

    // ── ffmpeg ishga tushirish ─────────────────────────────

    private void runFfmpeg(List<String> args, Duration timeout) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        if (isLinux() && Files.isExecutable(Path.of("/usr/bin/nice"))) {
            // Kichik VPS: API so'rovlariga CPU ustuvorligini qoldiramiz
            cmd.addAll(List.of("/usr/bin/nice", "-n", "10"));
        }
        cmd.add(ffmpegPath);
        cmd.addAll(List.of("-hide_banner", "-nostdin", "-loglevel", "error", "-y"));
        cmd.addAll(args);

        Process process;
        try {
            process = new ProcessBuilder(cmd)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException e) {
            markFfmpegMissing(e);
            throw new FfmpegMissingException();
        }
        currentProcess = process;
        StderrTail stderr = new StderrTail(process.getErrorStream());
        Thread drain = new Thread(stderr, "ffmpeg-stderr");
        drain.setDaemon(true);
        drain.start();
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IOException("ffmpeg vaqt limitidan oshdi (" + timeout.toMinutes() + " daqiqa)");
            }
            drain.join(2000);
            if (process.exitValue() != 0) {
                String tail = stderr.tail();
                throw new IOException("ffmpeg xatosi (kod " + process.exitValue() + ")"
                        + (tail.isBlank() ? "" : ": " + tail));
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            currentProcess = null;
        }
    }

    /** ffmpeg mavjudligini tekshiradi ({@code ffmpeg -version}). Yo'q bo'lsa bir marta ogohlantiradi. */
    private boolean ffmpegAvailable() {
        try {
            Process p = new ProcessBuilder(ffmpegPath, "-hide_banner", "-version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            boolean ok = p.exitValue() == 0;
            if (ok) ffmpegMissingLogged.set(false);
            return ok;
        } catch (IOException e) {
            markFfmpegMissing(e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void markFfmpegMissing(IOException e) {
        if (ffmpegMissingLogged.compareAndSet(false, true)) {
            log.warn("{} ('{}'): tejamkor video va faststart o'chiq. Serverda: apt install ffmpeg ({})",
                    FFMPEG_MISSING, ffmpegPath, e.getMessage());
        }
    }

    // ── Baza yordamchilari ─────────────────────────────────

    private void setStatus(Long lessonId, String videoUrl, String status, String error) {
        try {
            lessonRepository.updateVideoLowStatusIfVideo(lessonId, videoUrl, status, truncate(error))
                    .block(DB_TIMEOUT);
        } catch (Exception e) {
            log.warn("Lesson {}: video holatini yozib bo'lmadi: {}", lessonId, e.getMessage());
        }
    }

    /**
     * Natijani saqlaydi (faqat video_url hali {@code src} bo'lsa). Muvaffaqiyatli bo'lsa eski
     * tejamkor faylni (boshqa dars ishlatmasa) o'chiradi. Natija — saqlandimi.
     */
    private boolean finish(Long lessonId, String src, String lowPath, String status, String oldLow) {
        Integer updated = lessonRepository.saveVideoLowResult(lessonId, src, lowPath, status).block(DB_TIMEOUT);
        if (updated == null || updated == 0) {
            log.info("Lesson {}: video ish davomida almashtirildi — natija bekor qilindi", lessonId);
            return false;
        }
        if (oldLow != null && !oldLow.equals(lowPath)) deleteIfUnreferenced(oldLow);
        return true;
    }

    private void deleteIfUnreferenced(String path) {
        if (!Lesson.isLocalVideo(path)) return;
        try {
            Long refs = lessonRepository.countFileReferences(path).block(DB_TIMEOUT);
            if (refs != null && refs == 0) fileStorageService.deleteStoredFile(path);
        } catch (Exception e) {
            log.warn("Faylni o'chirib bo'lmadi ({}): {}", path, e.getMessage());
        }
    }

    // ── Fayl yordamchilari ─────────────────────────────────

    private static void ensureFreeSpace(Path dir, long needed) throws IOException {
        long usable = Files.getFileStore(dir).getUsableSpace();
        long reserve = 500L * 1024 * 1024; // diskni to'liq to'ldirib qo'ymaslik uchun zaxira
        if (usable < needed + reserve) {
            throw new IOException("Diskda joy yetarli emas (bo'sh: " + usable / (1024 * 1024) + " MB)");
        }
    }

    private static void deleteStaleParts(Path dir) {
        try (var files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".mp4.part")).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static void moveAtomic(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
        FileStorageService.makePublicReadable(to);
    }

    private static String newUid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase(Locale.ROOT) : "";
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    private static String truncate(String s) {
        if (s == null) return null;
        String trimmed = s.strip();
        return trimmed.length() <= ERROR_MAX_CHARS ? trimmed
                : trimmed.substring(trimmed.length() - ERROR_MAX_CHARS);
    }

    private static final class FfmpegMissingException extends IOException {
        FfmpegMissingException() {
            super(FFMPEG_MISSING);
        }
    }

    /** ffmpeg stderr'ini to'xtovsiz o'qiydi (bufer to'lib jarayon qotmasligi uchun), oxirgi qismini saqlaydi. */
    private static final class StderrTail implements Runnable {
        private static final int KEEP = 4096;
        private final InputStream in;
        private final StringBuilder buf = new StringBuilder();

        StderrTail(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            byte[] chunk = new byte[4096];
            try {
                int n;
                while ((n = in.read(chunk)) > 0) {
                    synchronized (buf) {
                        buf.append(new String(chunk, 0, n, StandardCharsets.UTF_8));
                        if (buf.length() > KEEP * 2) buf.delete(0, buf.length() - KEEP);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        String tail() {
            synchronized (buf) {
                String s = buf.toString().strip();
                return s.length() <= 400 ? s : s.substring(s.length() - 400);
            }
        }
    }
}
