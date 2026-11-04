package uz.sevenEdu.teacherBot.lesson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.lesson.dto.ExerciseAdminDto;
import uz.sevenEdu.teacherBot.lesson.dto.ExerciseBlankDto;
import uz.sevenEdu.teacherBot.lesson.dto.ExerciseUpsertRequest;
import uz.sevenEdu.teacherBot.lesson.entity.Exercise;
import uz.sevenEdu.teacherBot.lesson.entity.ExerciseBlank;
import uz.sevenEdu.teacherBot.lesson.repository.ExerciseBlankRepository;
import uz.sevenEdu.teacherBot.lesson.repository.ExerciseRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin: ko'p bo'shliqli mashqlar CRUD.
 * Gapdagi '_' guruhlari soni va to'g'ri javoblar soni mos bo'lishi tekshiriladi.
 */
@Service
@RequiredArgsConstructor
public class ExerciseAdminService {

    /** Gapdagi bo'shliq belgisi: bitta yoki bir nechta '_' ketma-ket = 1 bo'shliq. */
    public static final Pattern BLANK = Pattern.compile("_+");

    private final ExerciseRepository exerciseRepository;
    private final ExerciseBlankRepository blankRepository;

    public Flux<ExerciseAdminDto> list(Long lessonId) {
        return exerciseRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList()
                .flatMapMany(exercises -> {
                    if (exercises.isEmpty()) return Flux.empty();
                    List<Long> ids = exercises.stream().map(Exercise::getId).toList();
                    return blankRepository.findGroupedByExerciseIds(ids)
                            .flatMapMany(byEx -> Flux.fromIterable(exercises)
                                    .map(e -> toDto(e, byEx.getOrDefault(e.getId(), List.of()))));
                });
    }

    @Transactional
    public Mono<ExerciseAdminDto> create(Long lessonId, ExerciseUpsertRequest req) {
        return Mono.fromCallable(() -> validate(req.getSentence(), req.getCorrectAnswers()))
                .flatMap(answers -> {
                    Exercise ex = Exercise.builder()
                            .lessonId(lessonId)
                            .name(req.getName() != null && !req.getName().isBlank() ? req.getName().trim() : "Fill")
                            .orderIndex(req.getOrderIndex() != null ? req.getOrderIndex() : 0)
                            .sentence(req.getSentence().trim())
                            .options(cleanOptions(req.getOptions(), answers))
                            .build();
                    return exerciseRepository.save(ex)
                            .flatMap(saved -> saveBlanks(saved.getId(), answers)
                                    .map(blanks -> toDto(saved, blanks)));
                });
    }

    @Transactional
    public Mono<ExerciseAdminDto> update(Long id, ExerciseUpsertRequest req) {
        return exerciseRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Mashq topilmadi")))
                .flatMap(ex -> {
                    String sentence = req.getSentence() != null && !req.getSentence().isBlank()
                            ? req.getSentence().trim()
                            : ex.getSentence();
                    // Javoblar berilmasa mavjudlari saqlanadi (lekin yangi gap bilan mosligi tekshiriladi)
                    Mono<List<String>> answersMono = req.getCorrectAnswers() != null
                            ? Mono.just(req.getCorrectAnswers())
                            : blankRepository.findByExerciseIdOrderByPosition(id)
                                    .map(ExerciseBlank::getCorrectAnswer).collectList();
                    return answersMono.flatMap(raw -> {
                        List<String> answers = validate(sentence, raw);
                        ex.setSentence(sentence);
                        if (req.getName() != null && !req.getName().isBlank()) ex.setName(req.getName().trim());
                        if (req.getOrderIndex() != null) ex.setOrderIndex(req.getOrderIndex());
                        if (req.getOptions() != null) ex.setOptions(cleanOptions(req.getOptions(), answers));
                        return exerciseRepository.save(ex)
                                .flatMap(saved -> blankRepository.deleteByExerciseId(id)
                                        .then(saveBlanks(id, answers))
                                        .map(blanks -> toDto(saved, blanks)));
                    });
                });
    }

    /** exercise_blanks ON DELETE CASCADE bilan o'chadi. */
    public Mono<Void> delete(Long id) {
        return exerciseRepository.deleteById(id);
    }

    // ── Yordamchilar ────────────────────────────────────────────────────────

    /** Gapdagi '_' guruhlari soni. */
    public static int countBlanks(String sentence) {
        if (sentence == null) return 0;
        Matcher m = BLANK.matcher(sentence);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /** Gap va javoblarni tekshiradi; tozalangan (trim) javoblar ro'yxatini qaytaradi. */
    static List<String> validate(String sentence, List<String> rawAnswers) {
        if (sentence == null || sentence.isBlank()) {
            throw new BadRequestException("Gap bo'sh bo'lishi mumkin emas");
        }
        List<String> answers = rawAnswers == null
                ? List.of()
                : rawAnswers.stream().map(s -> s == null ? "" : s.trim()).toList();
        if (answers.isEmpty() || answers.stream().anyMatch(String::isEmpty)) {
            throw new BadRequestException("Har bir bo'shliq uchun to'g'ri so'z kiriting");
        }
        int blanks = countBlanks(sentence);
        // Eski ma'lumot: '_' qo'yilmagan va bitta javob — bo'shliq gap oxirida deb hisoblanadi
        if (blanks == 0 && answers.size() == 1) return answers;
        if (blanks != answers.size()) {
            throw new BadRequestException(
                    "Gapdagi bo'shliqlar soni (" + blanks + ") javoblar soniga (" + answers.size() + ") mos emas");
        }
        return answers;
    }

    private Mono<List<ExerciseBlank>> saveBlanks(Long exerciseId, List<String> answers) {
        List<ExerciseBlank> list = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            list.add(ExerciseBlank.builder()
                    .exerciseId(exerciseId)
                    .position(i)
                    .correctAnswer(answers.get(i))
                    .build());
        }
        return blankRepository.saveAll(list).collectList();
    }

    /** Chalg'ituvchilar: trim, bo'shlar va to'g'ri javoblarga tenglari (katta-kichik harf farqsiz) olib tashlanadi. */
    static String cleanOptions(String options, List<String> answers) {
        if (options == null) return "";
        Set<String> correctKeys = new LinkedHashSet<>();
        for (String a : answers) correctKeys.add(a.toLowerCase());
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (String raw : options.split(",")) {
            String o = raw.trim();
            if (o.isEmpty()) continue;
            String key = o.toLowerCase();
            if (correctKeys.contains(key) || !seen.add(key)) continue;
            out.add(o);
        }
        return String.join(",", out);
    }

    /** Student uchun variantlar: to'g'ri javoblar + chalg'ituvchilar (takrorsiz). Eski mobil ilovalar ham ishlashi uchun. */
    public static String studentOptions(String distractors, List<ExerciseBlank> blanks) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (ExerciseBlank b : blanks) {
            String a = b.getCorrectAnswer() == null ? "" : b.getCorrectAnswer().trim();
            if (!a.isEmpty() && seen.add(a.toLowerCase())) out.add(a);
        }
        if (distractors != null) {
            for (String raw : distractors.split(",")) {
                String o = raw.trim();
                if (!o.isEmpty() && seen.add(o.toLowerCase())) out.add(o);
            }
        }
        return String.join(",", out);
    }

    static ExerciseAdminDto toDto(Exercise e, List<ExerciseBlank> blanks) {
        List<String> answers = blanks.stream().map(ExerciseBlank::getCorrectAnswer).toList();
        return ExerciseAdminDto.builder()
                .id(e.getId())
                .lessonId(e.getLessonId())
                .name(e.getName())
                .orderIndex(e.getOrderIndex())
                .sentence(e.getSentence())
                .options(cleanOptions(e.getOptions(), answers))
                .blanks(blanks.stream().map(ExerciseBlankDto::from).toList())
                .build();
    }
}
