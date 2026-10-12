package uz.sevenEdu.teacherBot.payment.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;

/**
 * To'lov webhook'larida kelgan summani KURSNING haqiqiy (kutilgan) narxi
 * bilan solishtirish uchun YAGONA manba — BookPaymentAmountService'ning
 * kurslar uchun aynan shunday jufti. Barcha provider servislari
 * (Payme, Paynet, Click, Uzum Nasiya, Alif Nasiya) shu servisdan foydalanadi.
 *
 * DIQQAT: narx formulasi CheckoutService.createCourseCheckoutUrl dagi hisob
 * bilan AYNAN bir xil bo'lishi shart (kurs narxi, so'mda), aks holda
 * haqiqiy to'lovlar rad etiladi.
 *
 * Birliklar:
 *  - expectedAmountSum   — SO'M  (Click, Uzum Nasiya, Alif Nasiya)
 *  - expectedAmountTiyin — TIYIN (Payme, Paynet; 1 so'm = 100 tiyin)
 */
@Service
@RequiredArgsConstructor
public class CoursePaymentAmountService {

    private final CourseRepository courseRepository;

    /**
     * merchantTransId ("course_{userId}_{courseId}") dan courseId ni ajratadi.
     * Format noto'g'ri bo'lsa null qaytaradi.
     */
    public Long parseCourseId(String merchantTransId) {
        if (merchantTransId == null) return null;
        String[] parts = merchantTransId.split("_");
        if (parts.length != 3 || !"course".equals(parts[0])) return null;
        try {
            Long.parseLong(parts[1]); // userId ham raqam bo'lishi shart
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Kutilgan to'lov summasi SO'MDA.
     * Kurs topilmasa yoki merchantTransId formati noto'g'ri bo'lsa — bo'sh Mono.
     */
    public Mono<Long> expectedAmountSum(String merchantTransId) {
        Long courseId = parseCourseId(merchantTransId);
        if (courseId == null) return Mono.empty();
        return courseRepository.findById(courseId).map(this::priceSum);
    }

    /**
     * Kutilgan to'lov summasi TIYINDA (so'm × 100).
     */
    public Mono<Long> expectedAmountTiyin(String merchantTransId) {
        return expectedAmountSum(merchantTransId).map(sum -> sum * 100L);
    }

    /** CheckoutService.createCourseCheckoutUrl dagi formula bilan AYNAN bir xil. */
    private long priceSum(Course course) {
        return course.getPrice() != null ? course.getPrice() : 0L;
    }
}
