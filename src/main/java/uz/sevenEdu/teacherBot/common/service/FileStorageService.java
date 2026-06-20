package uz.sevenEdu.teacherBot.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.web.util.UriUtils;

import uz.sevenEdu.teacherBot.common.enums.LanguageFileType;
import uz.sevenEdu.teacherBot.common.enums.LessonFileType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

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

    /** Dars muqova (oboloshka) rasmini saqlash. Path: lessons/{lessonId}/cover_{uid}.{ext} */
    public Mono<String> saveLessonCover(Long lessonId, FilePart filePart, String oldPath) {
        deleteIfExists(oldPath);
        String ext = getExtension(filePart.filename());
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String fileName = "cover_" + lessonId + "_" + uid + ext;
        Path dest = basePath.resolve("lessons").resolve(String.valueOf(lessonId)).resolve(fileName);
        return saveFile(filePart, dest).thenReturn(basePath.relativize(dest).toString().replace("\\", "/"));
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
        createDirectories(dest.getParent());
        return filePart.transferTo(dest);
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
