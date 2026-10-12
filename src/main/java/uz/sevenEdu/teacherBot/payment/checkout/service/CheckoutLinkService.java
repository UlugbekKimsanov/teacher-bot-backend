package uz.sevenEdu.teacherBot.payment.checkout.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.entity.Books;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;
import uz.sevenEdu.teacherBot.books.repository.UserBookRepository;
import uz.sevenEdu.teacherBot.books.service.CheckoutService;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.payment.checkout.dto.CheckoutInfoDto;
import uz.sevenEdu.teacherBot.payment.checkout.dto.CheckoutLinkDto;
import uz.sevenEdu.teacherBot.payment.checkout.dto.CheckoutLinkRequest;
import uz.sevenEdu.teacherBot.payment.checkout.dto.CheckoutMethodDto;
import uz.sevenEdu.teacherBot.payment.checkout.entity.CheckoutToken;
import uz.sevenEdu.teacherBot.payment.checkout.repository.CheckoutTokenRepository;
import uz.sevenEdu.teacherBot.payment.common.BookPaymentAmountService;
import uz.sevenEdu.teacherBot.payment.entity.PaymentMethod;
import uz.sevenEdu.teacherBot.payment.repository.PaymentMethodRepository;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mobil → web checkout oqimi:
 *  1) Mobil ilova POST /api/v1/checkout-links bilan qisqa muddatli token oladi;
 *  2) Foydalanuvchi https://oazisedu.uz/?t=<token> sahifasini ochadi;
 *  3) Landing GET /api/landing/checkout/{token} bilan ma'lumotni ko'rsatadi
 *     (faqat o'qish — hech narsa consume qilinmaydi);
 *  4) POST /api/landing/checkout/{token}/pay provayder checkout URL'ini quradi.
 * Token to'lov muvaffaqiyatida (webhook) used_at bilan bekor qilinadi —
 * qayta ishlatilmaydi; to'lov amalga oshmasa retry mumkin.
 */
@Service
@RequiredArgsConstructor
public class CheckoutLinkService {

    /** OWASP magic-link tavsiyasi bo'yicha qisqa TTL. */
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final String LANDING_BASE_URL = "https://oazisedu.uz/?t=";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CheckoutTokenRepository checkoutTokenRepository;
    private final BooksRepository booksRepository;
    private final CourseRepository courseRepository;
    private final UserBookRepository userBookRepository;
    private final UserCourseRepository userCourseRepository;
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CheckoutService checkoutService;
    private final BookPaymentAmountService bookPaymentAmountService;

    // ── Token yaratish (mobil, Bearer auth bilan) ───────────────────

    public Mono<CheckoutLinkDto> createLink(Long userId, CheckoutLinkRequest request) {
        String productType = request.getProductType() == null
                ? "" : request.getProductType().trim().toLowerCase(Locale.ROOT);
        Long productId = request.getProductId();
        if (productId == null) {
            return Mono.error(new BadRequestException("productId majburiy"));
        }

        Mono<Void> validation = switch (productType) {
            case "book"   -> validateBookPurchase(userId, productId);
            case "course" -> validateCoursePurchase(userId, productId);
            default -> Mono.error(new BadRequestException(
                    "productType faqat 'course' yoki 'book' bo'lishi mumkin"));
        };

        return validation.then(Mono.defer(() -> saveToken(userId, productType, productId)));
    }

    /** Kitob: mavjud, bepul emas, foydalanuvchida faol xarid yozuvi yo'q. */
    private Mono<Void> validateBookPurchase(Long userId, Long bookId) {
        return booksRepository.findById(bookId)
                .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi")))
                .flatMap(book -> {
                    if (Boolean.TRUE.equals(book.getIsFree())
                            || book.getPrice() == null || book.getPrice() <= 0) {
                        return Mono.<Books>error(new BadRequestException(
                                "Bu kitob bepul — to'lov talab qilinmaydi"));
                    }
                    return userBookRepository.findByUserIdAndBookId(userId, bookId)
                            .filter(ub -> Boolean.TRUE.equals(ub.getIsActive()))
                            .flatMap(ub -> Mono.<Books>error(new BadRequestException(
                                    "Bu kitob allaqachon sotib olingan")))
                            .defaultIfEmpty(book);
                })
                .then();
    }

    /** Kurs: mavjud, premium, foydalanuvchida UserCourse yozuvi yo'q. */
    private Mono<Void> validateCoursePurchase(Long userId, Long courseId) {
        return courseRepository.findById(courseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course -> {
                    if (!Boolean.TRUE.equals(course.getIsPremium())) {
                        return Mono.<Course>error(new BadRequestException(
                                "Bu kurs bepul — to'lov talab qilinmaydi"));
                    }
                    return userCourseRepository.existsByUserIdAndCourseId(userId, courseId)
                            .flatMap(exists -> exists
                                    ? Mono.<Course>error(new BadRequestException(
                                            "Siz bu kursga allaqachon yozilgansiz"))
                                    : Mono.just(course));
                })
                .then();
    }

    private Mono<CheckoutLinkDto> saveToken(Long userId, String productType, Long productId) {
        LocalDateTime now = LocalDateTime.now();
        CheckoutToken entity = CheckoutToken.builder()
                .token(generateToken())
                .userId(userId)
                .productType(productType)
                .productId(productId)
                .createdAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .build();
        return checkoutTokenRepository.save(entity)
                .map(saved -> CheckoutLinkDto.builder()
                        .url(LANDING_BASE_URL + saved.getToken())
                        .token(saved.getToken())
                        .expiresAt(saved.getExpiresAt())
                        .build());
    }

    /** SecureRandom 32 bayt (256 bit) → URL-safe Base64, paddingsiz (43 belgi). */
    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ── Landing: checkout ma'lumoti (faqat o'qish) ──────────────────

    public Mono<CheckoutInfoDto> getCheckoutInfo(String token) {
        return requireValidToken(token).flatMap(t -> {
            Mono<String> userNameMono = userRepository.findById(t.getUserId())
                    .map(this::fullName)
                    .defaultIfEmpty("");
            Mono<List<PaymentMethod>> methodsMono = paymentMethodRepository
                    .findByEnabledTrueOrderByOrderIndexAsc()
                    .collectList();

            if ("course".equals(t.getProductType())) {
                return Mono.zip(
                                courseRepository.findById(t.getProductId())
                                        .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi"))),
                                userNameMono,
                                methodsMono)
                        .map(tuple -> courseInfo(t, tuple.getT1(), tuple.getT2(), tuple.getT3()));
            }
            // Jami summa — webhook'lardagi tekshiruv bilan AYNAN bir xil manba
            return Mono.zip(
                            booksRepository.findById(t.getProductId())
                                    .switchIfEmpty(Mono.error(new NotFoundException("Kitob topilmadi"))),
                            bookPaymentAmountService
                                    .expectedAmountSum("book_" + t.getUserId() + "_" + t.getProductId())
                                    .defaultIfEmpty(0L),
                            userNameMono,
                            methodsMono)
                    .map(tuple -> bookInfo(t, tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()));
        });
    }

    private CheckoutInfoDto courseInfo(CheckoutToken t, Course course,
                                       String userName, List<PaymentMethod> methods) {
        long amountSum = course.getPrice() != null ? course.getPrice() : 0L;
        String priceLabel = course.getPriceLabel() != null && !course.getPriceLabel().isBlank()
                ? course.getPriceLabel()
                : formatSum(amountSum);
        return CheckoutInfoDto.builder()
                .productType("course")
                .productId(t.getProductId())
                .title(course.getName())
                .priceLabel(priceLabel)
                .amountSum(amountSum)
                .userName(userName)
                // Kurs uchun faqat oddiy (nasiya bo'lmagan) usullar
                .methods(methods.stream()
                        .filter(m -> !Boolean.TRUE.equals(m.getIsNasiya()))
                        .map(this::toMethodDto)
                        .toList())
                .expiresAt(t.getExpiresAt())
                .build();
    }

    private CheckoutInfoDto bookInfo(CheckoutToken t, Books book, Long amountSum,
                                     String userName, List<PaymentMethod> methods) {
        return CheckoutInfoDto.builder()
                .productType("book")
                .productId(t.getProductId())
                .title(book.getTitle())
                .priceLabel(formatSum(amountSum))
                .amountSum(amountSum)
                .deliveryLabel(deliveryLabel(book))
                .userName(userName)
                .methods(methods.stream().map(this::toMethodDto).toList())
                .expiresAt(t.getExpiresAt())
                .build();
    }

    /** Bosma kitob uchun yetkazib berish yorlig'i; boshqa holatlarda null. */
    private String deliveryLabel(Books book) {
        if (!"print".equalsIgnoreCase(book.getCategory())) return null;
        String type = book.getDeliveryType();
        if (type == null) return null;
        return switch (type) {
            case "FREE" -> "Yetkazib berish: bepul";
            case "NEGOTIABLE" -> "Yetkazib berish: kelishiladi";
            case "PAID" -> book.getDeliveryPrice() != null && book.getDeliveryPrice() > 0
                    ? "Yetkazib berish: " + formatSum(book.getDeliveryPrice().longValue())
                    : null;
            default -> null;
        };
    }

    // ── Landing: provayder checkout URL ─────────────────────────────

    public Mono<Map<String, String>> pay(String token, String method) {
        if (method == null || method.isBlank()) {
            return Mono.error(new BadRequestException("To'lov usuli ko'rsatilmagan"));
        }
        // "uzum nasiya" → "uzumnasiya" kabi normalizatsiya
        String code = method.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        return requireValidToken(token).flatMap(t ->
                paymentMethodRepository.findByEnabledTrueOrderByOrderIndexAsc()
                        .filter(pm -> pm.getCode() != null && pm.getCode().equalsIgnoreCase(code))
                        .next()
                        .switchIfEmpty(Mono.error(new BadRequestException(
                                "Bu to'lov usuli yoqilmagan yoki mavjud emas")))
                        .flatMap(pm -> buildCheckoutUrl(t, pm)));
    }

    private Mono<Map<String, String>> buildCheckoutUrl(CheckoutToken t, PaymentMethod pm) {
        if ("course".equals(t.getProductType())) {
            if (Boolean.TRUE.equals(pm.getIsNasiya())) {
                return Mono.error(new BadRequestException(
                        "Kurs uchun nasiya to'lov usuli qo'llab-quvvatlanmaydi"));
            }
            return checkoutService.createCourseCheckoutUrl(t.getUserId(), t.getProductId(), pm.getCode())
                    .map(res -> Map.of("checkoutUrl", res.get("checkoutUrl")));
        }
        // Kitob — MAVJUD checkout logikasi qayta ishlatiladi
        return checkoutService.createCheckoutUrl(t.getUserId(), t.getProductId(), pm.getCode())
                .map(res -> Map.of("checkoutUrl", res.get("checkoutUrl")));
    }

    // ── Yordamchi metodlar ──────────────────────────────────────────

    /**
     * Token mavjud, hali ishlatilmagan (used_at IS NULL) va muddati o'tmagan
     * bo'lishi shart. Hech narsa consume qilinmaydi (link-scanner'lardan himoya).
     */
    private Mono<CheckoutToken> requireValidToken(String token) {
        if (token == null || token.isBlank()) {
            return Mono.error(new NotFoundException("To'lov havolasi topilmadi"));
        }
        return checkoutTokenRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new NotFoundException("To'lov havolasi topilmadi")))
                .flatMap(t -> {
                    if (t.getUsedAt() != null) {
                        return Mono.<CheckoutToken>error(new NotFoundException(
                                "Bu havola orqali to'lov allaqachon amalga oshirilgan"));
                    }
                    if (t.getExpiresAt() == null || t.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return Mono.<CheckoutToken>error(new NotFoundException(
                                "Havola muddati tugagan — ilovadan yangi havola oching"));
                    }
                    return Mono.just(t);
                });
    }

    private String fullName(BaseUser user) {
        String name = (user.getFirstName() != null ? user.getFirstName() : "")
                + " " + (user.getLastName() != null ? user.getLastName() : "");
        return name.trim();
    }

    private CheckoutMethodDto toMethodDto(PaymentMethod pm) {
        return CheckoutMethodDto.builder()
                .code(pm.getCode())
                .name(pm.getName())
                .isNasiya(Boolean.TRUE.equals(pm.getIsNasiya()))
                .build();
    }

    /** 199000 → "199 000 so'm" */
    private String formatSum(long sum) {
        return String.format(Locale.US, "%,d", sum).replace(",", " ") + " so'm";
    }
}
