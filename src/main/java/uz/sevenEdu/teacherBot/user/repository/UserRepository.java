package uz.sevenEdu.teacherBot.user.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;

public interface UserRepository extends ReactiveCrudRepository<BaseUser, Long> {
    Mono<BaseUser> findByPhone(String phone);
    Mono<BaseUser> findByEmail(String email);
    Mono<Boolean> existsByPhone(String phone);
    Mono<Boolean> existsByEmail(String email);

    @Query("SELECT * FROM users WHERE role = :role")
    Flux<BaseUser> findByRole(String role);

    /** isDefault=false admin uchun: faqat TEACHER va STUDENT */
    @Query("SELECT * FROM users WHERE role IN ('TEACHER', 'STUDENT')")
    Flux<BaseUser> findTeachersAndStudents();

    /** isDefault=false admin uchun: rol filtri bilan (faqat TEACHER yoki STUDENT) */
    @Query("SELECT * FROM users WHERE role = :role AND role IN ('TEACHER', 'STUDENT')")
    Flux<BaseUser> findTeachersAndStudentsByRole(String role);

    /** isDefault=true admin uchun: o'zidan tashqari hammani */
    @Query("SELECT * FROM users WHERE id != :excludeId")
    Flux<BaseUser> findAllExcept(Long excludeId);

    /** isDefault=true admin uchun: rol filtri + o'zidan tashqari */
    @Query("SELECT * FROM users WHERE role = :role AND id != :excludeId")
    Flux<BaseUser> findByRoleExcept(String role, Long excludeId);
}
