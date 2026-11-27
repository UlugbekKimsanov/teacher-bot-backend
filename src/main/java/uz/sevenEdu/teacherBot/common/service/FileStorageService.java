package uz.sevenEdu.teacherBot.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.web.util.UriUtils;

import uz.sevenEdu.teacherBot.common.enums.LanguageFileType;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.PayloadTooLargeException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    static final int MAX_LANDING_IMAGE_BYTES = 5 * 1024 * 1024;

    /**
     * Dars videosi uchun maksimal hajm — 2 GiB.
     * Nginx'dagi {@code client_max_body_size} bilan bir xil bo'lishi kerak
     * (deploy/api.oazisedu.uz.conf), aks holda 413 nginx'dan qaytib,
     * bizning xato matnimiz foydalanuvchiga yetib bormaydi.
     */
    public static final long MAX_VIDEO_BYTES = 2L * 1024 * 1024 * 1024;

    private static final String VIDEO_TOO_LARGE_MESSAGE = "Video hajmi 2 GB dan oshmasligi kerak";

    private final Path basePath;

    public FileStorageService(@Value("${app.storage.base-path}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    // ── Yuklash turlari ─────────────────────────────────────

    /**
     * Yuklanadigan fayl turi: ruxsat etilgan kengaytmalar va maksimal hajm.
     * Admin paneldagi chegaralar shu qiymatlardan oshmasligi kerak.
     */
    public enum UploadKind {
        IMAGE(Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif"), 10L * 1024 * 1024,
                "Faqat JPG, PNG, WebP yoki GIF rasm qabul qilinadi",
                "Rasm hajmi 10 MB dan oshmasligi kerak"),
        AUDIO(Set.of(".mp3", ".m4a", ".aac", ".wav", ".ogg", ".oga", ".opus", ".flac"), 200L * 1024 * 1024,
                "Faqat audio fayl (MP3, M4A, AAC, WAV, OGG, OPUS, FLAC) qabul qilinadi",
                "Audio hajmi 200 MB dan oshmasligi kerak"),
        VIDEO(Set.of(".mp4", ".m4v", ".mov", ".webm", ".mkv", ".avi"), MAX_VIDEO_BYTES,
                "Faqat video fayl (MP4, MOV, WebM, MKV, AVI) qabul qilinadi",
                VIDEO_TOO_LARGE_MESSAGE),
        BOOK(Set.of(".pdf", ".epub"), 200L * 1024 * 1024,
                "Faqat PDF yoki EPUB fayl qabul qilinadi",
                "Kitob fayli hajmi 200 MB dan oshmasligi kerak"),
        PDF(Set.of(".pdf"), 200L * 1024 * 1024,
                "Faqat PDF fayl qabul qilinadi",
                "PDF hajmi 200 MB dan oshmasligi kerak");

        private final Set<String> extensions;
        private final long maxBytes;
        private final String typeMessage;
        private final String sizeMessage;

        UploadKind(Set<String> extensions, long maxBytes, String typeMessage, String sizeMessage) {
            this.extensions = extensions;
            this.maxBytes = maxBytes;
            this.typeMessage = typeMessage;
            this.sizeMessage = sizeMessage;
        }
    }

    /**
     * Barcha yuklashlar uchun yagona yo'l:
     *  1) kengaytma va (ma'lum bo'lsa) Content-Length tekshiriladi — bitta bayt yozilmasdan;
     *  2) fayl yangi nom bilan yoziladi, yozilgandan keyin haqiqiy hajm tekshiriladi;
     *  3) FAQAT muvaffaqiyatdan keyin eski fayl o'chiriladi (yuklash uzilsa eskisi saqlanib qoladi).
     * Natija — bazaga yoziladigan nisbiy yo'l.
     */
    private Mono<String> store(FilePart filePart, UploadKind kind, Path dir, String baseName, String oldPath) {
        return Mono.defer(() -> {
            if (filePart == null || filePart.filename() == null || filePart.filename().isBlank()) {
                return Mono.error(new BadRequestException("Fayl tanlanmagan"));
            }
            String ext = getExtension(filePart.filename()).toLowerCase(Locale.ROOT);
            if (!kind.extensions.contains(ext)) {
                return Mono.error(new BadRequestException(kind.typeMessage));
            }
            if (filePart.headers().getContentLength() > kind.maxBytes) {
                return Mono.error(new PayloadTooLargeException(kind.sizeMessage));
            }
            String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            Path dest = dir.resolve(baseName + "_" + uid + ext).normalize();
            if (!dest.startsWith(basePath)) {
                return Mono.error(new BadRequestException("Fayl yo'li noto'g'ri"));
            }
            String relative = basePath.relativize(dest).toString().replace("\\", "/");
            return saveFile(filePart, dest)
                    .then(enforceSize(dest, kind.maxBytes, kind.sizeMessage))
                    .onErrorResume(e -> Mono.fromRunnable(() -> deleteQuietly(dest))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then(Mono.error(e)))
                    .then(Mono.fromRunnable(() -> {
                        if (oldPath != null && !oldPath.equals(relative)) deleteIfExists(oldPath);
                    }).subscribeOn(Schedulers.boundedElastic()))
                    .thenReturn(relative);
        });
    }

    // ── Language ────────────────────────────────────────────

    /**
     * Language yaratilganda: {languageName}_{languageId}/courses/
     */
    public void createLanguageFolder(String languageName, Long languageId) {
        Path langDir = languagePath(languageName, languageId);
        createDirectories(langDir.resolve("courses"));
    }

    /**
     * Language uchun flag yoki background rasm saqlash
     * fileType: "flag" yoki "background"
     */
    public Mono<String> saveLanguageImage(String languageName, Long languageId,
                                          LanguageFileType fileType, FilePart filePart,
                                          String oldPath) {
        String typeName = fileType.name().toLowerCase(Locale.ROOT);
        return store(filePart, UploadKind.IMAGE, languagePath(languageName, languageId),
                languageName + "_" + typeName + "_" + languageId, oldPath);
    }

    // ── Book ──────────────────────────────────────────────

    /** Kitob fayli (PDF/EPUB). Path: books/{bookId}/book_{bookId}_{uid}.{ext} */
    public Mono<String> saveBookFile(Long bookId, FilePart filePart, String oldPath) {
        return store(filePart, UploadKind.BOOK, entityDir("books", bookId), "book_" + bookId, oldPath);
    }

    /** Kitob muqova rasmi. Path: books/{bookId}/cover_{bookId}_{uid}.{ext} */
    public Mono<String> saveBookCover(Long bookId, FilePart filePart, String oldPath) {
        return store(filePart, UploadKind.IMAGE, entityDir("books", bookId), "cover_" + bookId, oldPath);
    }

    /** Kitob galereya rasmi (eski fayl o'chirilmaydi — bir nechta rasm saqlanadi). */
    public Mono<String> saveBookImage(Long bookId, FilePart filePart) {
        return store(filePart, UploadKind.IMAGE, entityDir("books", bookId), "img_" + bookId, null);
    }

    /** Saqlangan faylni nisbiy yo'l bo'yicha o'chirish. Storage'dan tashqaridagi yo'llar e'tiborsiz qoldiriladi. */
    public void deleteStoredFile(String relativePath) {
        deleteIfExists(relativePath);
    }

    /** {@link #deleteStoredFile} ning event-loop'ni bloklamaydigan varianti (bir nechta yo'l). Xato yutiladi. */
    public Mono<Void> deleteStoredFiles(java.util.Collection<String> relativePaths) {
        if (relativePaths == null || relativePaths.isEmpty()) return Mono.empty();
        return Mono.<Void>fromRunnable(() -> relativePaths.forEach(this::deleteIfExists))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> Mono.empty());
    }

    // ── Course ─────────────────────────────────────────────

    /** Kurs card (cover) rasmi. Path: courses/{courseId}/cover_{courseId}_{uid}.{ext} */
    public Mono<String> saveCourseCover(Long courseId, FilePart filePart, String oldPath) {
        return store(filePart, UploadKind.IMAGE, entityDir("courses", courseId), "cover_" + courseId, oldPath);
    }

    /** Kurs background rasmi. Path: courses/{courseId}/bg_{courseId}_{uid}.{ext} */
    public Mono<String> saveCourseBackground(Long courseId, FilePart filePart, String oldPath) {
        return store(filePart, UploadKind.IMAGE, entityDir("courses", courseId), "bg_" + courseId, oldPath);
    }

    // ── Tanaffus musiqasi ──────────────────────────────────

    /** Tanaffus guruhi fon rasmi. Path: break/{groupId}/bg_{groupId}_{uid}.{ext} */
    public Mono<String> saveBreakBackground(Long groupId, FilePart filePart, String oldPath) {
        return store(filePart, UploadKind.IMAGE, entityDir("break", groupId), "bg_" + groupId, oldPath);
    }

    /** Tanaffus musiqasi. Path: break/{groupId}/track_{groupId}_{uid}.{ext} */
    public Mono<String> saveBreakTrack(Long groupId, FilePart filePart) {
        return store(filePart, UploadKind.AUDIO, entityDir("break", groupId), "track_" + groupId, null);
    }

    // ── Lesson ─────────────────────────────────────────────

    /** Dars videosi (maks. 2 GB). Path: lessons/{lessonId}/video_{lessonId}_{uid}.{ext} */
    public Mono<String> saveLessonVideo(Long lessonId, FilePart filePart, String oldPath) {
        return store(filePart, UploadKind.VIDEO, entityDir("lessons", lessonId), "video_" + lessonId, oldPath);
    }

    /** Absolute path — ffprobe kabi tashqi vositalar uchun. DB'dagi relative path'dan yasaydi. */
    public Path resolveStoredPath(String dbPath) {
        if (dbPath == null || dbPath.isBlank()) return null;
        Path path = Paths.get(dbPath);
        Path resolved = (path.isAbsolute() ? path : basePath.resolve(dbPath)).normalize();
        return resolved.startsWith(basePath) ? resolved : null;
    }

    /** Dars fayllari papkasi (absolute): lessons/{lessonId}/ */
    public Path lessonDir(Long lessonId) {
        return entityDir("lessons", lessonId);
    }

    /** Storage ichidagi absolute path -> bazaga yoziladigan nisbiy yo'l. Storage'dan tashqarida bo'lsa null. */
    public String toStoredPath(Path absolute) {
        if (absolute == null) return null;
        Path normalized = absolute.toAbsolutePath().normalize();
        if (!normalized.startsWith(basePath)) return null;
        return basePath.relativize(normalized).toString().replace("\\", "/");
    }

    /**
     * Lokal fayl hajmi (bayt). Tashqi URL, storage'dan tashqaridagi yo'l yoki fayl
     * mavjud bo'lmasa — null. BLOKLOVCHI (stat) — boundedElastic'da chaqiring.
     */
    public Long storedFileSize(String dbPath) {
        if (dbPath == null || dbPath.isBlank()) return null;
        if (dbPath.startsWith("http://") || dbPath.startsWith("https://")) return null;
        try {
            Path path = resolveStoredPath(dbPath);
            if (path == null || !Files.isRegularFile(path)) return null;
            return Files.size(path);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Dars audio kitobi. Path: lessons/{lessonId}/audio_{lessonId}_{uid}.{ext} */
    public Mono<String> saveLessonAudiobook(Long lessonId, FilePart filePart) {
        return store(filePart, UploadKind.AUDIO, entityDir("lessons", lessonId), "audio_" + lessonId, null);
    }

    /** Audio kitob PDF fayli. Path: lessons/{lessonId}/audiopdf_{lessonId}_{uid}.pdf */
    public Mono<String> saveLessonAudiobookPdf(Long lessonId, FilePart filePart) {
        return store(filePart, UploadKind.PDF, entityDir("lessons", lessonId), "audiopdf_" + lessonId, null);
    }

    /** Dars muqova (oboloshka) rasmi. Path: lessons/{lessonId}/cover_{lessonId}_{uid}.{ext} */
    public Mono<String> saveLessonCover(Long lessonId, FilePart filePart, String oldPath) {
        return store(filePart, UploadKind.IMAGE, entityDir("lessons", lessonId), "cover_" + lessonId, oldPath);
    }

    /** Admin bildirishnoma rasmi. Path: notifications/notif_{uid}.{ext} */
    public Mono<String> saveNotificationImage(FilePart filePart) {
        return store(filePart, UploadKind.IMAGE, basePath.resolve("notifications"), "notif", null);
    }

    /** Landing sahifasi rasmini saqlash. Path: landing/{uid}.{ext} */
    public Mono<String> saveLandingImage(FilePart filePart) {
        if (filePart == null || filePart.filename() == null || filePart.filename().isBlank()) {
            return Mono.error(new BadRequestException("Rasm fayli tanlanmagan"));
        }

        return DataBufferUtils.join(filePart.content(), MAX_LANDING_IMAGE_BYTES)
                .onErrorMap(DataBufferLimitException.class,
                        ex -> new PayloadTooLargeException("Landing rasmi 5 MiB dan oshmasligi kerak"))
                .switchIfEmpty(Mono.error(new BadRequestException("Bo'sh rasm fayli qabul qilinmaydi")))
                .flatMap(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    try {
                        buffer.read(bytes);
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                    return Mono.fromCallable(() -> saveValidatedLandingImage(filePart, bytes))
                            .subscribeOn(Schedulers.boundedElastic());
                });
    }

    private String saveValidatedLandingImage(FilePart filePart, byte[] bytes) {
        if (bytes.length == 0) throw new BadRequestException("Bo'sh rasm fayli qabul qilinmaydi");
        if (bytes.length > MAX_LANDING_IMAGE_BYTES) {
            throw new PayloadTooLargeException("Landing rasmi 5 MiB dan oshmasligi kerak");
        }

        LandingImageType detected = detectLandingImage(bytes);
        String extension = getExtension(filePart.filename()).toLowerCase(Locale.ROOT);
        if (!detected.extensions.contains(extension)) {
            throw new BadRequestException("Rasm kengaytmasi fayl tarkibiga mos emas");
        }

        MediaType declaredType = filePart.headers().getContentType();
        String declaredMime = declaredType == null
                ? ""
                : (declaredType.getType() + "/" + declaredType.getSubtype()).toLowerCase(Locale.ROOT);
        if (!detected.mime.equals(declaredMime)) {
            throw new BadRequestException("Rasm MIME turi fayl tarkibiga mos emas");
        }

        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Path dest = basePath.resolve("landing").resolve("landing_" + uid + detected.canonicalExtension);
        createDirectories(dest.getParent());
        try {
            Files.write(dest, bytes, StandardOpenOption.CREATE_NEW);
            makePublicReadable(dest);
        } catch (IOException ex) {
            throw new RuntimeException("Landing rasmini saqlab bo'lmadi", ex);
        }
        return basePath.relativize(dest).toString().replace("\\", "/");
    }

    private LandingImageType detectLandingImage(byte[] bytes) {
        if (isJpeg(bytes)) return LandingImageType.JPEG;
        if (isPng(bytes)) return LandingImageType.PNG;
        if (isWebp(bytes)) return LandingImageType.WEBP;
        throw new BadRequestException("Faqat haqiqiy JPEG, PNG yoki WebP rasm qabul qilinadi");
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 4
                && unsigned(bytes[0]) == 0xFF && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF
                && unsigned(bytes[bytes.length - 2]) == 0xFF
                && unsigned(bytes[bytes.length - 1]) == 0xD9;
    }

    private boolean isPng(byte[] bytes) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        int[] iend = {0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82};
        return bytes.length >= 45
                && matches(bytes, 0, signature)
                && matchesAscii(bytes, 12, "IHDR")
                && matches(bytes, bytes.length - iend.length, iend);
    }

    private boolean isWebp(byte[] bytes) {
        if (bytes.length < 20
                || !matchesAscii(bytes, 0, "RIFF")
                || !matchesAscii(bytes, 8, "WEBP")) {
            return false;
        }
        long declaredSize = Integer.toUnsignedLong(
                unsigned(bytes[4])
                        | (unsigned(bytes[5]) << 8)
                        | (unsigned(bytes[6]) << 16)
                        | (unsigned(bytes[7]) << 24));
        if (declaredSize + 8 != bytes.length) return false;
        return matchesAscii(bytes, 12, "VP8 ")
                || matchesAscii(bytes, 12, "VP8L")
                || matchesAscii(bytes, 12, "VP8X");
    }

    private boolean matches(byte[] bytes, int offset, int[] expected) {
        if (offset < 0 || offset + expected.length > bytes.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if (unsigned(bytes[offset + i]) != expected[i]) return false;
        }
        return true;
    }

    private boolean matchesAscii(byte[] bytes, int offset, String expected) {
        if (offset < 0 || offset + expected.length() > bytes.length) return false;
        for (int i = 0; i < expected.length(); i++) {
            if (unsigned(bytes[offset + i]) != expected.charAt(i)) return false;
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private enum LandingImageType {
        JPEG("image/jpeg", ".jpg", Set.of(".jpg", ".jpeg")),
        PNG("image/png", ".png", Set.of(".png")),
        WEBP("image/webp", ".webp", Set.of(".webp"));

        private final String mime;
        private final String canonicalExtension;
        private final Set<String> extensions;

        LandingImageType(String mime, String canonicalExtension, Set<String> extensions) {
            this.mime = mime;
            this.canonicalExtension = canonicalExtension;
            this.extensions = extensions;
        }
    }

    // ── Path → URL ─────────────────────────────────────────

    /**
     * DB dagi path ni public URL ga aylantiradi.
     * Eski absolute pathlar ham, yangi relative pathlar ham ishlaydi.
     * Natija: /files/languages/English_1/file.png
     */
    public String toPublicUrl(String dbPath) {
        if (dbPath == null || dbPath.isBlank()) return null;
        // Allaqachon to'liq URL bo'lsa (masalan seed dagi tashqi video) — o'zgartirmaymiz
        if (dbPath.startsWith("http://") || dbPath.startsWith("https://")) return dbPath;
        String normalized = dbPath.replace("\\", "/");
        // Agar absolute path bo'lsa, basePath ni olib tashlaymiz
        String baseStr = basePath.toString().replace("\\", "/");
        if (normalized.startsWith(baseStr)) {
            normalized = normalized.substring(baseStr.length());
            if (normalized.startsWith("/")) normalized = normalized.substring(1);
        }
        // URL-enkod (bo'sh joy -> %20 va h.k.), '/' saqlanadi — aks holda probelli
        // papka nomlari (masalan "Ingliz tili_1") URL'da buziladi
        return "/files/" + UriUtils.encodePath(normalized, StandardCharsets.UTF_8);
    }

    // ── File yuklash (GET) ─────────────────────────────────

    public Mono<Resource> loadFile(String filePath) {
        return Mono.fromCallable(() -> {
            Path path = basePath.resolve(filePath).normalize();
            if (!path.startsWith(basePath)) {
                throw new RuntimeException("Ruxsat berilmagan yo'l: " + filePath);
            }
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("Fayl topilmadi: " + filePath);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ── Path helpers ───────────────────────────────────────

    private Path languagePath(String languageName, Long languageId) {
        return basePath.resolve("languages").resolve(languageName + "_" + languageId);
    }

    private Path entityDir(String folder, Long id) {
        return basePath.resolve(folder).resolve(String.valueOf(id));
    }

    /** Faqat storage ichidagi faylni o'chiradi; tashqi URL va storage'dan tashqaridagi yo'llarga tegmaydi. */
    private void deleteIfExists(String oldPath) {
        if (oldPath == null || oldPath.isBlank()) return;
        if (oldPath.startsWith("http://") || oldPath.startsWith("https://")) return;
        try {
            Path path = Paths.get(oldPath);
            Path resolved = (path.isAbsolute() ? path : basePath.resolve(oldPath)).normalize();
            if (!resolved.startsWith(basePath) || resolved.equals(basePath)) return;
            if (Files.isRegularFile(resolved)) Files.deleteIfExists(resolved);
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    /**
     * Fayl diskka yozilgandan keyin haqiqiy hajmni tekshiradi (Content-Length yuborilmagan
     * yuklashlar uchun yagona ishonchli himoya). Bo'sh fayl — 400, chegaradan katta — 413.
     * Faylni o'chirish chaqiruvchida (store) bajariladi.
     */
    private Mono<Void> enforceSize(Path dest, long maxBytes, String message) {
        return Mono.fromCallable(() -> {
                    long size = Files.size(dest);
                    if (size == 0) throw new BadRequestException("Bo'sh fayl qabul qilinmaydi");
                    if (size > maxBytes) throw new PayloadTooLargeException(message);
                    return true;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private Mono<Void> saveFile(FilePart filePart, Path dest) {
        // Bloklovchi papka yaratishni event-loop'dan boundedElastic'ga ko'chiramiz.
        return Mono.fromRunnable(() -> createDirectories(dest.getParent()))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .then(filePart.transferTo(dest))
                .then(Mono.fromRunnable(() -> makePublicReadable(dest))
                        .subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    /**
     * Faylga 644 (rw-r--r--) huquqini beradi. Multipart vaqtinchalik fayl 600 bilan yaratiladi —
     * shunda nginx (www-data) /files/ ni diskdan to'g'ridan-to'g'ri bera olmaydi (403).
     * POSIX bo'lmagan tizimda (Windows) jim o'tkazib yuboriladi.
     */
    public static void makePublicReadable(Path file) {
        try {
            Files.setPosixFilePermissions(file, PUBLIC_FILE_PERMS);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows / huquqni o'zgartirib bo'lmadi — fayl baribir backend orqali berila oladi
        }
    }

    private static final Set<java.nio.file.attribute.PosixFilePermission> PUBLIC_FILE_PERMS =
            java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--");

    private void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Papka yaratib bo'lmadi: " + dir, e);
        }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
