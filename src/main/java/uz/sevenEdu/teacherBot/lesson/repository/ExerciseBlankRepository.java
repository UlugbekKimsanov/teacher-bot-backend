package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.lesson.entity.ExerciseBlank;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ExerciseBlankRepository extends ReactiveCrudRepository<ExerciseBlank, Long> {

    Flux<ExerciseBlank> findByExerciseIdOrderByPosition(Long exerciseId);

    Flux<ExerciseBlank> findByExerciseIdInOrderByExerciseIdAscPositionAsc(Collection<Long> exerciseIds);

    Mono<Void> deleteByExerciseId(Long exerciseId);

    /**
     * Bir nechta mashqning bo'shliqlari: exerciseId → position bo'yicha tartiblangan ro'yxat.
     * Bo'sh id ro'yxati uchun so'rov yuborilmaydi (R2DBC `IN ()` xatosi oldini olish).
     */
    default Mono<Map<Long, List<ExerciseBlank>>> findGroupedByExerciseIds(Collection<Long> exerciseIds) {
        if (exerciseIds == null || exerciseIds.isEmpty()) return Mono.just(Map.of());
        return findByExerciseIdInOrderByExerciseIdAscPositionAsc(exerciseIds)
                .collectList()
                .map(list -> list.stream().collect(Collectors.groupingBy(
                        ExerciseBlank::getExerciseId, LinkedHashMap::new, Collectors.toList())));
    }
}
