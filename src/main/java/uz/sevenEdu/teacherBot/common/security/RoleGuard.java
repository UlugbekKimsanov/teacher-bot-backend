package uz.sevenEdu.teacherBot.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

/**
 * Umumiy rol tekshiruvi — rolni DB'dan o'qiydi
 * (AdminController.requireAdmin uslubida).
 */
@Component
@RequiredArgsConstructor
public class RoleGuard {

    private final UserRepository userRepository;

    /** Faqat ADMIN. Muvaffaqiyatda userId qaytaradi. */
    public Mono<Long> requireAdmin(Authentication auth) {
        return requireRole(auth, "Faqat admin uchun", UserRole.ADMIN);
    }

    /** TEACHER yoki ADMIN. Muvaffaqiyatda userId qaytaradi. */
    public Mono<Long> requireTeacherOrAdmin(Authentication auth) {
        return requireRole(auth, "Faqat ustoz uchun", UserRole.TEACHER, UserRole.ADMIN);
    }

    /** Foydalanuvchi ADMIN'mi (egalik tekshiruvlari uchun). */
    public Mono<Boolean> isAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .defaultIfEmpty(false);
    }

    private Mono<Long> requireRole(Authentication auth, String message, UserRole... roles) {
        if (auth == null) {
            return Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi"));
        }
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi")))
                .flatMap(user -> {
                    for (UserRole role : roles) {
                        if (user.getRole() == role) return Mono.just(userId);
                    }
                    return Mono.error(new ForbiddenException(message));
                });
    }
}
