package uz.sevenEdu.teacherBot.course.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.entity.CourseGiftBook;

public interface CourseGiftBookRepository extends ReactiveCrudRepository<CourseGiftBook, Long> {

    Flux<CourseGiftBook> findByCourseId(Long courseId);

    Mono<CourseGiftBook> findByCourseIdAndBookId(Long courseId, Long bookId);

    /** Kursning sovg'a kitoblari nomlari (biriktirilgan tartibda). */
    @Query("SELECT b.title FROM course_gift_books g JOIN books b ON b.id = g.book_id "
            + "WHERE g.course_id = :courseId ORDER BY g.id")
    Flux<String> findGiftBookTitles(Long courseId);

    /** Bog'lanishni qo'shadi; allaqachon bo'lsa hech narsa qilmaydi. */
    @Modifying
    @Query("INSERT INTO course_gift_books (course_id, book_id, created_at) VALUES (:courseId, :bookId, NOW()) "
            + "ON CONFLICT (course_id, book_id) DO NOTHING")
    Mono<Integer> link(Long courseId, Long bookId);

    @Modifying
    @Query("DELETE FROM course_gift_books WHERE course_id = :courseId AND book_id = :bookId")
    Mono<Integer> unlink(Long courseId, Long bookId);

    /**
     * Kursning barcha sovg'a kitoblarini bitta o'quvchining kutubxonasiga qo'shadi
     * (kurs sotib olinganda). Kitob allaqachon bo'lsa tegilmaydi.
     */
    @Modifying
    @Query("INSERT INTO user_books (user_id, book_id, payment_method, is_active, purchased_at) "
            + "SELECT :userId, g.book_id, 'gift', TRUE, NOW() FROM course_gift_books g WHERE g.course_id = :courseId "
            + "ON CONFLICT (user_id, book_id) DO NOTHING")
    Mono<Integer> grantCourseGiftsToUser(Long userId, Long courseId);

    /**
     * Bitta kitobni kursning barcha mavjud o'quvchilariga sovg'a qiladi
     * (kitob kursga keyinroq biriktirilganda). Kitobi borlarga tegilmaydi.
     */
    @Modifying
    @Query("INSERT INTO user_books (user_id, book_id, payment_method, is_active, purchased_at) "
            + "SELECT uc.user_id, :bookId, 'gift', TRUE, NOW() FROM user_courses uc WHERE uc.course_id = :courseId "
            + "ON CONFLICT (user_id, book_id) DO NOTHING")
    Mono<Integer> grantBookToCourseStudents(Long courseId, Long bookId);
}
