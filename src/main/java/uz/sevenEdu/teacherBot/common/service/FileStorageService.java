package uz.sevenEdu.teacherBot.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import uz.sevenEdu.teacherBot.common.enums.LanguageFileType;
import uz.sevenEdu.teacherBot.common.enums.LessonFileType;

import java.io.IOException;
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
        return saveFile(filePart, dest).thenReturn(dest.toString());
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
        return saveFile(filePart, dest).thenReturn(dest.toString());
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
        return saveFile(filePart, dest).thenReturn(dest.toString());
    }

    // ── File yuklash (GET) ─────────────────────────────────

    public Mono<Resource> loadFile(String filePath) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("Fayl topilmadi: " + filePath);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ── Path helpers ───────────────────────────────────────

    private Path languagePath(String languageName, Long languageId) {
        return basePath.resolve(languageName + "_" + languageId);
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
                Files.deleteIfExists(Paths.get(oldPath));
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
