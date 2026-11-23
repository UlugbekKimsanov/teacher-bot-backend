package uz.sevenEdu.teacherBot.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.course.entity.CourseGiftBook;
import uz.sevenEdu.teacherBot.course.repository.CourseGiftBookRepository;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.util.List;
import java.util.Map;

/**
 * Kursga sovg'a kitob biriktirish (admin). Bitta bog'lanish jadvali — admin panelda
 * ikki tomondan boshqariladi: kursdan kitob tanlash yoki kitobdan kurs tanlash.
 *
 * Qoidalar:
 *  - faqat pullik (premium) kursga biriktiriladi — sovg'a xarid uchun beriladi;
 *  - faqat elektron kitob biriktiriladi (bosma kitob yetkazib berishni talab qiladi);
 *  - biriktirilganda kursning mavjud o'quvchilari ham kitobni oladi;
 *  - bog'lanish olib tashlansa, avval berilgan kitoblar qaytarib olinmaydi.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCourseGiftController {

    private final CourseGiftBookRepository giftRepository;
    private final CourseRepository courseRepository;
    private final BooksRepository booksRepository;
    private final UserRepository userRepository;

    /** Barcha bog'lanishlar: [{ id, courseId, bookId, createdAt }]. */
    @GetMapping("/course-gift-books")
    public Mono<ApiResponse<List<CourseGiftBook>>> list(Authentication auth) {
        return requireAdmin(auth).then(giftRepository.findAll().collectList().map(ApiResponse::ok));
    }

    /** Kitobni kursga sovg'a sifatida biriktiradi. Javob data: { "granted": <kitobni olgan mavjud o'quvchilar soni> }. */
    @PostMapping("/courses/{courseId}/gift-books/{bookId}")
    public Mono<ApiResponse<Map<String, Integer>>> link(Authentication auth,
                                                        @PathVariable Long courseId,
                                                        @PathVariable Long bookId) {
        return requireAdmin(auth).then(Mono.defer(() -> Mono.zip(
                        courseRepository.findById(courseId)
                                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi"))),
                        booksRepository.findById(bookId)
                                .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi"))))
                .flatMap(pair -> {
                    if (!Boolean.TRUE.equals(pair.getT1().getIsPremium())) {
                        return Mono.error(new BadRequestException(
                                "Sovg'a kitob faqat pullik kursga biriktiriladi"));
                    }
                    if ("print".equalsIgnoreCase(pair.getT2().getCategory())) {
                        return Mono.error(new BadRequestException(
                                "Bosma kitobni sovg'a qilib bo'lmaydi — faqat elektron kitob biriktiriladi"));
                    }
                    return giftRepository.link(courseId, bookId)
                            .then(giftRepository.grantBookToCourseStudents(courseId, bookId))
                            .defaultIfEmpty(0);
                })
                .map(granted -> ApiResponse.ok("Biriktirildi", Map.of("granted", granted)))));
    }

    /** Bog'lanishni olib tashlaydi (avval berilgan kitoblar o'quvchilarda qoladi). */
    @DeleteMapping("/courses/{courseId}/gift-books/{bookId}")
    public Mono<ApiResponse<Void>> unlink(Authentication auth,
                                          @PathVariable Long courseId,
                                          @PathVariable Long bookId) {
        return requireAdmin(auth)
                .then(giftRepository.unlink(courseId, bookId))
                .then(Mono.just(ApiResponse.ok("Olib tashlandi", (Void) null)));
    }

    private Mono<Void> requireAdmin(Authentication auth) {
        if (auth == null) return Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi"));
        Long userId = (Long) auth.getPrincipal();
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Avtorizatsiya talab qilinadi")))
                .flatMap(user -> user.getRole() != UserRole.ADMIN
                        ? Mono.<Void>error(new ForbiddenException("Faqat admin uchun"))
                        : Mono.<Void>empty());
    }
}
