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
import uz.sevenEdu.teacherBot.common.enums.LessonFileType;
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

    private final Path basePath;

    public FileStorageService(@Value("${app.storage.base-path}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
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
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String typeName = fileType.name().toLowerCase();
        String fileName = languageName + "_" + typeName + "_" + languageId + "_" + uid + ext;
        Path dest = languagePath(languageName, languageId).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    // ── Course ─────────────────────────────────────────────

    /**
     * Course yaratilganda: {lang}/courses/{courseName}_{courseId}/lessons/
     */
    public void createCourseFolder(String languageName, Long languageId,
                                   String courseName, Long courseId) {
        Path courseDir = coursePath(languageName, languageId, courseName, courseId);
        createDirectories(courseDir.resolve("lessons"));
    }

    /**
     * Course uchun cover rasm saqlash
     */
    public Mono<String> saveCourseImage(String languageName, Long languageId,
                                        String courseName, Long courseId,
                                        FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = courseName + "_cover_" + courseId + "_" + uid + ext;
        Path dest = coursePath(languageName, languageId, courseName, courseId).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    // ── Lesson ─────────────────────────────────────────────

    /**
     * Lesson yaratilganda: {course}/lessons/{lessonName}_{lessonId}/
     */
    public void createLessonFolder(String languageName, Long languageId,
                                   String courseName, Long courseId,
                                   String lessonName, Long lessonId) {
        Path lessonDir = lessonPath(languageName, languageId, courseName, courseId, lessonName, lessonId);
        createDirectories(lessonDir);
    }

    /**
     * Lesson uchun cover yoki video saqlash
     * fileType: "cover" yoki "video"
     */
    public Mono<String> saveLessonFile(String languageName, Long languageId,
                                       String courseName, Long courseId,
                                       String lessonName, Long lessonId,
                                       LessonFileType fileType, FilePart filePart,
                                       String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String typeName = fileType.name().toLowerCase();
        String fileName = lessonName + "_" + typeName + "_" + lessonId + "_" + uid + ext;
        Path dest = lessonPath(languageName, languageId, courseName, courseId, lessonName, lessonId)
                .resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    // ── Book ──────────────────────────────────────────────

    /**
     * Kitob fayli saqlash (PDF, EPUB va boshqalar)
     * Path: books/{bookId}/{fileName}
     */
    public Mono<String> saveBookFile(Long bookId, FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "book_" + bookId + "_" + uid + ext;
        Path dest = basePath.resolve("books").resolve(String.valueOf(bookId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /**
     * Kitob muqova rasmi saqlash
     * Path: books/{bookId}/cover_{bookId}_{uid}.{ext}
     */
    public Mono<String> saveBookCover(Long bookId, FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "cover_" + bookId + "_" + uid + ext;
        Path dest = basePath.resolve("books").resolve(String.valueOf(bookId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /**
     * Kurs card (cover) rasmi saqlash
     * Path: courses/{courseId}/cover_{courseId}_{uid}.{ext}
     */
    public Mono<String> saveCourseCover(Long courseId, FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "cover_" + courseId + "_" + uid + ext;
        Path dest = basePath.resolve("courses").resolve(String.valueOf(courseId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /**
     * Kurs background rasmi saqlash
     * Path: courses/{courseId}/bg_{courseId}_{uid}.{ext}
     */
    public Mono<String> saveCourseBackground(Long courseId, FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "bg_" + courseId + "_" + uid + ext;
        Path dest = basePath.resolve("courses").resolve(String.valueOf(courseId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /** Tanaffus guruhi fon rasmini saqlash. Path: break/{groupId}/bg_{uid}.{ext} */
    public Mono<String> saveBreakBackground(Long groupId, FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "bg_" + groupId + "_" + uid + ext;
        Path dest = basePath.resolve("break").resolve(String.valueOf(groupId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /** Tanaffus musiqasini saqlash. Path: break/{groupId}/track_{uid}.{ext} */
    public Mono<String> saveBreakTrack(Long groupId, FilePart filePart) {
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "track_" + groupId + "_" + uid + ext;
        Path dest = basePath.resolve("break").resolve(String.valueOf(groupId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /**
     * Dars videosini saqlash
     * Path: lessons/{lessonId}/video_{lessonId}_{uid}.{ext}
     */
    public Mono<String> saveLessonVideo(Long lessonId, FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "video_" + lessonId + "_" + uid + ext;
        Path dest = basePath.resolve("lessons").resolve(String.valueOf(lessonId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /** Dars audio kitobini saqlash. Path: lessons/{lessonId}/audio_{uid}.{ext} */
    public Mono<String> saveLessonAudiobook(Long lessonId, FilePart filePart) {
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "audio_" + lessonId + "_" + uid + ext;
        Path dest = basePath.resolve("lessons").resolve(String.valueOf(lessonId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /** Audio kitob PDF faylini saqlash. Path: lessons/{lessonId}/audiopdf_{uid}.{ext} */
    public Mono<String> saveLessonAudiobookPdf(Long lessonId, FilePart filePart) {
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "audiopdf_" + lessonId + "_" + uid + ext;
        Path dest = basePath.resolve("lessons").resolve(String.valueOf(lessonId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /** Dars muqova (oboloshka) rasmini saqlash. Path: lessons/{lessonId}/cover_{uid}.{ext} */
    public Mono<String> saveLessonCover(Long lessonId, FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "cover_" + lessonId + "_" + uid + ext;
        Path dest = basePath.resolve("lessons").resolve(String.valueOf(lessonId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
    }

    /** Admin bildirishnoma rasmini saqlash. Path: notifications/{uid}.{ext} */
    public Mono<String> saveNotificationImage(FilePart filePart) {
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 12);
        String fileName = "notif_" + uid + ext;
        Path dest = basePath.resolve("notifications").resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
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

    private Path coursePath(String languageName, Long languageId,
                            String courseName, Long courseId) {
        return languagePath(languageName, languageId)
                .resolve("courses")
                .resolve(courseName + "_" + courseId);
    }

    private Path lessonPath(String languageName, Long languageId,
                            String courseName, Long courseId,
                            String lessonName, Long lessonId) {
        return coursePath(languageName, languageId, courseName, courseId)
                .resolve("lessons")
                .resolve(lessonName + "_" + lessonId);
    }

    private void deleteIfExists(String oldPath) {
        if (oldPath != null && !oldPath.isBlank()) {
            try {
                Path path = Paths.get(oldPath);
                if (!path.isAbsolute()) {
                    path = basePath.resolve(oldPath);
                }
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }

    private Mono<Void> saveFile(FilePart filePart, Path dest) {
        // Bloklovchi papka yaratishni event-loop'dan boundedElastic'ga ko'chiramiz.
        return Mono.fromRunnable(() -> createDirectories(dest.getParent()))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .then(filePart.transferTo(dest));
    }

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
