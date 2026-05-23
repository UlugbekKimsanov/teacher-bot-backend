package uz.sevenEdu.teacherBot.books.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.dto.BooksDto;
import uz.sevenEdu.teacherBot.books.entity.Books;
import uz.sevenEdu.teacherBot.books.entity.UserBook;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;
import uz.sevenEdu.teacherBot.books.repository.UserBookRepository;

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
    private final uz.sevenEdu.teacherBot.common.service.FileStorageService fileStorageService;
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
                    if (userId == null) return Mono.just(mapToDto(book, false, false));
                    return userBookRepository.findByUserIdAndBookId(userId, id)
                            .map(ub -> mapToDto(book,
                                    true,
                                    ub.getIsActive() != null && ub.getIsActive()))
                            .defaultIfEmpty(mapToDto(book, false, false));
                });
    }

    public Mono<Void> purchaseBook(Long userId, Long bookId) {
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
                            .paymentMethod("card")
                            .isActive(true)
                            .purchasedAt(LocalDateTime.now())
                            .build())
                ))
                .then();
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
        return booksRepository.save(book).map(b -> mapToDto(b, false, false));
    }

    public Flux<BooksDto> getAllBooks() {
        return booksRepository.findAll().map(b -> mapToDto(b, false, false));
    }

    public Mono<BooksDto> updateBook(Long id, BooksDto dto) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(existing -> {
                    existing.setTitle(dto.getTitle());
                    existing.setAuthor(dto.getAuthor());
                    existing.setCategory(dto.getCategory());
                    existing.setDescription(dto.getDescription());
                    existing.setPrice(dto.getPrice());
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
                .format(entity.getFormat())
                .rating(entity.getRating())
                .reviewCount(entity.getReviewCount())
                .language(entity.getLanguage())
                .level(entity.getLevel())
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
                .format(dto.getFormat())
                .rating(dto.getRating())
                .reviewCount(dto.getReviewCount())
                .language(dto.getLanguage())
                .level(dto.getLevel())
                .previewPages(previewJson)
                .imageId(dto.getImageId())
                .fileId(dto.getFileId())
                .build();
    }
}
