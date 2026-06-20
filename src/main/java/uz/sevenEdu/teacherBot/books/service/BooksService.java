package uz.sevenEdu.teacherBot.books.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.dto.BooksDto;
import uz.sevenEdu.teacherBot.books.entity.Books;
import uz.sevenEdu.teacherBot.books.entity.SaleRecord;
import uz.sevenEdu.teacherBot.books.entity.UserBook;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;
import uz.sevenEdu.teacherBot.books.repository.SaleRecordRepository;
import uz.sevenEdu.teacherBot.books.repository.UserBookRepository;
import uz.sevenEdu.teacherBot.notification.service.NotificationService;
import uz.sevenEdu.teacherBot.telegram.TelegramBotService;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BooksService {
    private final BooksRepository booksRepository;
    private final UserBookRepository userBookRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TelegramBotService telegramBotService;
    private final uz.sevenEdu.teacherBot.common.service.FileStorageService fileStorageService;
    private final uz.sevenEdu.teacherBot.books.repository.BookRatingRepository bookRatingRepository;
    private static final ObjectMapper mapper = new ObjectMapper();

    public Flux<BooksDto> getBooksByCategory(String category, Long userId) {
        Flux<Books> booksFlux = booksRepository.findByCategory(category);
        if (userId == null) {
            return booksFlux.map(b -> mapToDto(b, false, false));
        }
        return booksFlux.collectList().flatMapMany(books -> {
            List<Long> bookIds = books.stream().map(Books::getId).toList();
            return userBookRepository.findByUserId(userId).collectList().flatMapMany(userBooks -> {
                // isPurchased = record exists (active or not) → user has paid/claimed
                Set<Long> purchasedIds = userBooks.stream()
                        .map(UserBook::getBookId).collect(Collectors.toSet());
                // inLibrary = record exists AND is_active = true
                Set<Long> activeIds = userBooks.stream()
                        .filter(ub -> ub.getIsActive() != null && ub.getIsActive())
                        .map(UserBook::getBookId).collect(Collectors.toSet());
                return Flux.fromIterable(books)
                        .map(b -> mapToDto(b, purchasedIds.contains(b.getId()), activeIds.contains(b.getId())));
            });
        });
    }

    public Mono<BooksDto> getBookById(Long id, Long userId) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(book -> {
                    Mono<BooksDto> base;
                    if (userId == null) {
                        base = Mono.just(mapToDto(book, false, false));
                    } else {
                        base = userBookRepository.findByUserIdAndBookId(userId, id)
                                .map(ub -> mapToDto(book, true, ub.getIsActive() != null && ub.getIsActive()))
                                .defaultIfEmpty(mapToDto(book, false, false));
                    }
                    if (userId == null) return base;
                    // Joriy foydalanuvchi qo'ygan bahoni qo'shamiz
                    return base.flatMap(dto -> bookRatingRepository.findByUserIdAndBookId(userId, id)
                            .map(r -> { dto.setMyRating(r.getRating()); return dto; })
                            .defaultIfEmpty(dto));
                });
    }

    // ── Reyting (foydalanuvchi baholashi) ──────────────────────────
    // Yangi kitob 5.0 dan boshlanadi (50 ta 5 baho seed). Har bir haqiqiy baho
    // bitta "5"ni almashtiradi; 50 tadan oshgach — sof o'rtacha arifmetik.
    public Mono<BooksDto> rateBook(Long userId, Long bookId, int rating) {
        final int r = Math.max(1, Math.min(5, rating));
        LocalDateTime now = LocalDateTime.now();
        return bookRatingRepository.findByUserIdAndBookId(userId, bookId)
                .flatMap(existing -> {
                    existing.setRating(r);
                    existing.setUpdatedAt(now);
                    return bookRatingRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> bookRatingRepository.save(
                        uz.sevenEdu.teacherBot.books.entity.BookRating.builder()
                                .userId(userId).bookId(bookId).rating(r)
                                .createdAt(now).updatedAt(now).build())))
                .then(recomputeRating(bookId))
                .map(dto -> { dto.setMyRating(r); return dto; });
    }

    private Mono<BooksDto> recomputeRating(Long bookId) {
        return Mono.zip(
                        bookRatingRepository.countByBookId(bookId),
                        bookRatingRepository.sumByBookId(bookId))
                .flatMap(t -> {
                    long count = t.getT1();
                    long sum = t.getT2();
                    double rating = (count <= 50)
                            ? (sum + (50 - count) * 5.0) / 50.0   // qolgan 5 lar bilan
                            : (double) sum / count;               // sof o'rtacha
                    double rounded = Math.round(rating * 10.0) / 10.0;
                    return booksRepository.findById(bookId)
                            .flatMap(b -> {
                                b.setRating(rounded);
                                b.setReviewCount((int) count);
                                return booksRepository.save(b);
                            })
                            .map(b -> mapToDto(b, false, false));
                });
    }

    /**
     * Eski chaqiruvlar uchun (backward compatible)
     */
    public Mono<Void> purchaseBook(Long userId, Long bookId) {
        return purchaseBook(userId, bookId, "card", null);
    }

    /**
     * To'liq sotuv qaydini yaratuvchi purchaseBook.
     * SaleRecord saqlaydi va admin larga notification yuboradi.
     */
    public Mono<Void> purchaseBook(Long userId, Long bookId, String paymentMethod, String transactionId) {
        return userBookRepository.findByUserIdAndBookId(userId, bookId)
                .flatMap(existing -> {
                    if (existing.getIsActive() == null || !existing.getIsActive()) {
                        return userBookRepository.setActive(userId, bookId, true).then(Mono.just(existing));
                    }
                    return Mono.just(existing);
                })
                .switchIfEmpty(Mono.defer(() ->
                    userBookRepository.save(UserBook.builder()
                            .userId(userId).bookId(bookId)
                            .paymentMethod(paymentMethod)
                            .isActive(true)
                            .purchasedAt(LocalDateTime.now())
                            .build())
                ))
                .then(recordSaleAndNotify(userId, bookId, paymentMethod, transactionId));
    }

    /**
     * Sotuv qaydini saqlash va admin larga notification yuborish
     */
    private Mono<Void> recordSaleAndNotify(Long userId, Long bookId, String paymentMethod, String transactionId) {
        return Mono.zip(
                booksRepository.findById(bookId).defaultIfEmpty(Books.builder().title("Noma'lum").price(0).build()),
                userRepository.findById(userId)
        ).flatMap(tuple -> {
            Books book = tuple.getT1();
            var user = tuple.getT2();
            String buyerName = (user.getFirstName() != null ? user.getFirstName() : "")
                    + " " + (user.getLastName() != null ? user.getLastName() : "");
            int amount = book.getPrice() != null ? book.getPrice() : 0;

            SaleRecord sale = SaleRecord.builder()
                    .userId(userId)
                    .bookId(bookId)
                    .bookTitle(book.getTitle())
                    .buyerName(buyerName.trim())
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .transactionId(transactionId)
                    .createdAt(LocalDateTime.now())
                    .build();

            return saleRecordRepository.save(sale)
                    .then(notifyAdmins(buyerName.trim(), book.getTitle(), amount, paymentMethod));
        });
    }

    /**
     * Barcha admin larga sotuv haqida notification yuborish:
     * 1) Saytdagi notification tizimiga (NotificationService)
     * 2) Telegram botga (TelegramBotService)
     * Ikkalasi parallel ishlaydi.
     */
    private Mono<Void> notifyAdmins(String buyerName, String bookTitle, int amount, String paymentMethod) {
        String title = "Yangi sotuv!";
        String body = buyerName + " — \"" + bookTitle + "\" kitobini "
                + paymentMethod + " orqali sotib oldi. Summa: " + amount + " so'm";

        // Telegram uchun HTML formatlangan xabar
        String telegramMsg = "<b>Yangi sotuv!</b>\n"
                + "Xaridor: " + buyerName + "\n"
                + "Kitob: <i>" + bookTitle + "</i>\n"
                + "Summa: <b>" + amount + " so'm</b>\n"
                + "To'lov: " + paymentMethod;

        // Sayt notification va Telegram parallel yuboriladi
        Mono<Void> siteNotification = userRepository.findByRole(UserRole.ADMIN.name())
                .concatMap(admin -> notificationService.send(
                        admin.getId(), title, body, "SALE", null))
                .then();

        Mono<Void> telegramNotification = telegramBotService.notifyAdmins(telegramMsg);

        return Mono.when(siteNotification, telegramNotification);
    }

    public Mono<Void> addToLibrary(Long userId, Long bookId) {
        return userBookRepository.findByUserIdAndBookId(userId, bookId)
                .flatMap(existing -> {
                    return userBookRepository.setActive(userId, bookId, true).then(Mono.just(existing));
                })
                .switchIfEmpty(Mono.defer(() ->
                    userBookRepository.save(UserBook.builder()
                            .userId(userId).bookId(bookId)
                            .paymentMethod("free")
                            .isActive(true)
                            .purchasedAt(LocalDateTime.now())
                            .build())
                ))
                .then();
    }

    public Mono<Void> removeFromLibrary(Long userId, Long bookId) {
        return userBookRepository.setActive(userId, bookId, false);
    }

    public Mono<Void> updateProgress(Long userId, Long bookId, int readPage, int totalPages) {
        // Mehmon — kitob o'qish progressi qayd etilmaydi
        if (uz.sevenEdu.teacherBot.common.util.GuestUtil.isGuest(userId)) return Mono.empty();
        return userBookRepository.updateProgress(userId, bookId, readPage, totalPages);
    }

    public Flux<BooksDto> getMyBooks(Long userId) {
        return userBookRepository.findByUserIdAndIsActive(userId, true).collectList()
                .flatMapMany(userBooks -> {
                    if (userBooks.isEmpty()) return Flux.empty();
                    Map<Long, UserBook> ubMap = userBooks.stream()
                            .collect(Collectors.toMap(UserBook::getBookId, ub -> ub));
                    List<Long> bookIds = userBooks.stream()
                            .map(UserBook::getBookId).toList();
                    return booksRepository.findAllById(bookIds)
                            .map(b -> {
                                UserBook ub = ubMap.get(b.getId());
                                return mapToDto(b, true, true,
                                        ub != null ? ub.getReadPage() : 0,
                                        ub != null ? ub.getTotalPages() : 0);
                            });
                });
    }

    // ── Admin CRUD (keep existing) ──────────────────────────────────

    public Mono<BooksDto> createBook(BooksDto dto) {
        Books book = mapToEntity(dto);
        book.setCreatedAt(LocalDateTime.now());
        // Yangi kitob 5.0 dan boshlanadi (50 ta 5 baho asosida)
        book.setRating(5.0);
        book.setReviewCount(0);
        return booksRepository.save(book).map(b -> mapToDto(b, false, false));
    }

    public Flux<BooksDto> getAllBooks() {
        return booksRepository.findAll().map(b -> mapToDto(b, false, false));
    }

    public Mono<BooksDto> updateBook(Long id, BooksDto dto) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(existing -> {
                    if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
                    if (dto.getAuthor() != null) existing.setAuthor(dto.getAuthor());
                    if (dto.getCategory() != null) existing.setCategory(dto.getCategory());
                    if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
                    if (dto.getPrice() != null) existing.setPrice(dto.getPrice());
                    if (dto.getPriceLabel() != null) existing.setPriceLabel(dto.getPriceLabel());
                    if (dto.getIsFree() != null) existing.setIsFree(dto.getIsFree());
                    if (dto.getEmoji() != null) existing.setEmoji(dto.getEmoji());
                    if (dto.getCoverColors() != null && !dto.getCoverColors().isEmpty()) {
                        existing.setCoverColor1(dto.getCoverColors().get(0));
                        if (dto.getCoverColors().size() > 1) existing.setCoverColor2(dto.getCoverColors().get(1));
                    }
                    if (dto.getPages() != null) existing.setPages(dto.getPages());
                    if (dto.getPageCount() != null) existing.setPageCount(dto.getPageCount());
                    // Reyting endi qo'lda emas — foydalanuvchi baholari asosida hisoblanadi
                    if (dto.getLanguage() != null) existing.setLanguage(dto.getLanguage());
                    if (dto.getPreviewPages() != null) {
                        try { existing.setPreviewPages(mapper.writeValueAsString(dto.getPreviewPages())); }
                        catch (Exception ignored) {}
                    }
                    if (dto.getImageId() != null) existing.setImageId(dto.getImageId());
                    if (dto.getFileId() != null) existing.setFileId(dto.getFileId());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return booksRepository.save(existing);
                })
                .map(b -> mapToDto(b, false, false));
    }

    public Mono<BooksDto> deleteBookAndReturn(Long id) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(book -> booksRepository.deleteById(id)
                        .then(Mono.just(mapToDto(book, false, false))));
    }

    // ── Mappers ─────────────────────────────────────────────────────

    private BooksDto mapToDto(Books entity, boolean isPurchased, boolean inLibrary) {
        return mapToDto(entity, isPurchased, inLibrary, 0, 0);
    }

    private BooksDto mapToDto(Books entity, boolean isPurchased, boolean inLibrary, int readPage, int totalPages) {
        List<Integer> coverColors = new ArrayList<>();
        if (entity.getCoverColor1() != null) coverColors.add(entity.getCoverColor1());
        if (entity.getCoverColor2() != null) coverColors.add(entity.getCoverColor2());
        if (coverColors.isEmpty()) {
            coverColors.add(0xFF2E9E6E);
            coverColors.add(0xFF1F7A55);
        }

        List<String> previewPages = new ArrayList<>();
        if (entity.getPreviewPages() != null && !entity.getPreviewPages().isBlank()) {
            try {
                previewPages = mapper.readValue(entity.getPreviewPages(),
                        new TypeReference<List<String>>() {});
            } catch (Exception ignored) {}
        }

        return BooksDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .priceLabel(entity.getPriceLabel())
                .isFree(entity.getIsFree() != null && entity.getIsFree())
                .emoji(entity.getEmoji() != null ? entity.getEmoji() : "\uD83D\uDCDA")
                .coverColors(coverColors)
                .pages(entity.getPages())
                .pageCount(entity.getPageCount())
                .rating(entity.getRating())
                .reviewCount(entity.getReviewCount())
                .language(entity.getLanguage())
                .coverUrl(fileStorageService.toPublicUrl(entity.getCoverImage()))
                .previewPages(previewPages)
                .isPurchased(isPurchased)
                .inLibrary(inLibrary)
                .readPage(readPage)
                .totalPages(totalPages)
                .imageId(entity.getImageId())
                .fileId(entity.getFileId())
                .fileUrl(fileStorageService.toPublicUrl(entity.getFilePath()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Books mapToEntity(BooksDto dto) {
        List<Integer> colors = dto.getCoverColors();
        String previewJson = null;
        if (dto.getPreviewPages() != null) {
            try { previewJson = mapper.writeValueAsString(dto.getPreviewPages()); }
            catch (Exception ignored) {}
        }
        return Books.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .priceLabel(dto.getPriceLabel())
                .isFree(dto.getIsFree())
                .emoji(dto.getEmoji())
                .coverColor1(colors != null && !colors.isEmpty() ? colors.get(0) : null)
                .coverColor2(colors != null && colors.size() > 1 ? colors.get(1) : null)
                .pages(dto.getPages())
                .pageCount(dto.getPageCount())
                .rating(dto.getRating())
                .reviewCount(dto.getReviewCount())
                .language(dto.getLanguage())
                .previewPages(previewJson)
                .imageId(dto.getImageId())
                .fileId(dto.getFileId())
                .build();
    }
}
