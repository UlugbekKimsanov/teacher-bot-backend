package uz.sevenEdu.teacherBot.books.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.dto.BooksDto;
import uz.sevenEdu.teacherBot.books.service.BooksService;
import uz.sevenEdu.teacherBot.books.service.CheckoutService;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.common.security.RoleGuard;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BooksController {
    private final BooksService booksService;
    private final CheckoutService checkoutService;
    private final RoleGuard roleGuard;

    @GetMapping("/category/{category}")
    public Mono<ApiResponse<List<BooksDto>>> getByCategory(
            @PathVariable String category, Authentication auth) {
        Long userId = getUserId(auth);
        return booksService.getBooksByCategory(category, userId)
                .collectList().map(ApiResponse::ok);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<BooksDto>> getById(
            @PathVariable Long id, Authentication auth) {
        Long userId = getUserId(auth);
        return booksService.getBookById(id, userId).map(ApiResponse::ok);
    }

    /**
     * To'lovsiz to'g'ridan-to'g'ri xarid taqiqlangan — kitob faqat to'lov
     * tizimi (webhook) orqali sotib olinadi. Endpoint eski APK'lar uchun qoladi.
     */
    @PostMapping("/{id}/purchase")
    public Mono<ApiResponse<String>> purchase(
            @PathVariable Long id, Authentication auth) {
        requireUserId(auth);
        return Mono.error(new ForbiddenException("Kitob faqat to'lov tizimi orqali sotib olinadi"));
    }

    /**
     * To'lov checkout URL olish.
     * paymentMethod: Click, Payme, Paynet, UzumNasiya, AlifNasiya
     * Qaytaradi: { "checkoutUrl": "https://...", "merchantTransId": "book_1_5" }
     */
    @PostMapping("/{id}/checkout")
    public Mono<ApiResponse<Map<String, String>>> checkout(
            @PathVariable Long id,
            @RequestParam String paymentMethod,
            Authentication auth) {
        Long userId = requireUserId(auth);
        return checkoutService.createCheckoutUrl(userId, id, paymentMethod)
                .map(ApiResponse::ok);
    }

    @PostMapping("/{id}/progress")
    public Mono<ApiResponse<String>> updateProgress(
            @PathVariable Long id,
            @RequestParam int readPage,
            @RequestParam int totalPages,
            Authentication auth) {
        Long userId = requireUserId(auth);
        return booksService.updateProgress(userId, id, readPage, totalPages)
                .then(Mono.just(ApiResponse.ok("Progress saqlandi", null)));
    }

    @PostMapping("/{id}/rate")
    public Mono<ApiResponse<BooksDto>> rate(
            @PathVariable Long id,
            @RequestParam int rating,
            Authentication auth) {
        Long userId = requireUserId(auth);
        return booksService.rateBook(userId, id, rating).map(ApiResponse::ok);
    }

    @PostMapping("/{id}/add-to-library")
    public Mono<ApiResponse<String>> addToLibrary(
            @PathVariable Long id, Authentication auth) {
        Long userId = requireUserId(auth);
        return booksService.addToLibrary(userId, id)
                .then(Mono.just(ApiResponse.ok("Kutubxonaga qo'shildi", null)));
    }

    @DeleteMapping("/{id}/remove-from-library")
    public Mono<ApiResponse<String>> removeFromLibrary(
            @PathVariable Long id, Authentication auth) {
        Long userId = requireUserId(auth);
        return booksService.removeFromLibrary(userId, id)
                .then(Mono.just(ApiResponse.ok("Kutubxonadan olib tashlandi", null)));
    }

    @GetMapping("/my")
    public Mono<ApiResponse<List<BooksDto>>> myBooks(Authentication auth) {
        Long userId = requireUserId(auth);
        return booksService.getMyBooks(userId).collectList().map(ApiResponse::ok);
    }

    // ── Admin endpoints ──────────────────────────────────────────

    @GetMapping
    public Mono<ApiResponse<List<BooksDto>>> getAll() {
        return booksService.getAllBooks().collectList().map(ApiResponse::ok);
    }

    @PostMapping
    public Mono<ApiResponse<BooksDto>> create(@RequestBody BooksDto dto, Authentication auth) {
        return roleGuard.requireAdmin(auth)
                .then(booksService.createBook(dto).map(ApiResponse::ok));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<BooksDto>> update(
            @PathVariable Long id, @RequestBody BooksDto dto, Authentication auth) {
        return roleGuard.requireAdmin(auth)
                .then(booksService.updateBook(id, dto).map(ApiResponse::ok));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<BooksDto>> delete(@PathVariable Long id, Authentication auth) {
        return roleGuard.requireAdmin(auth)
                .then(booksService.deleteBookAndReturn(id).map(ApiResponse::ok));
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }

    /** Token yo'q/yaroqsiz bo'lsa 500 (NPE) emas, 401 qaytadi. */
    private Long requireUserId(Authentication auth) {
        if (auth == null) throw new UnauthorizedException("Tizimga kirish talab etiladi");
        return (Long) auth.getPrincipal();
    }
}
