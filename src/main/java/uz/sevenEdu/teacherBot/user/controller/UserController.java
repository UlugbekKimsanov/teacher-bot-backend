package uz.sevenEdu.teacherBot.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.dto.AuthResponse;
import uz.sevenEdu.teacherBot.user.dto.UpdateProfileRequest;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;
import uz.sevenEdu.teacherBot.user.security.JwtUtil;
import uz.sevenEdu.teacherBot.user.util.PhoneNumberUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final DatabaseClient databaseClient;

    @PutMapping("/profile")
    public Mono<ApiResponse<AuthResponse>> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest request) {

        Long userId = extractUserId(authHeader);
        return userRepository.findById(userId)
                .flatMap(user -> {
                    if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
                    if (request.getLastName() != null) user.setLastName(request.getLastName());
                    if (request.getPhone() != null) {
                        user.setPhone(PhoneNumberUtil.normalizeOptionalUzbekPhone(request.getPhone()));
                    }
                    if (request.getAddress() != null) user.setAddress(request.getAddress());
                    return userRepository.save(user);
                })
                .map(user -> ApiResponse.ok(toResponse(user)));
    }

    @PostMapping("/avatar")
    public Mono<ApiResponse<String>> uploadAvatar(
            @RequestHeader("Authorization") String authHeader,
            @RequestPart("file") FilePart file) {

        Long userId = extractUserId(authHeader);
        String ext = getExtension(file.filename());
        String fileName = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path dir = Paths.get("./storage/avatars");

        return Mono.fromCallable(() -> {
                    Files.createDirectories(dir);
                    return dir.resolve(fileName);
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .flatMap(path -> file.transferTo(path)
                        .then(reactor.core.publisher.Mono.fromRunnable(() -> uz.sevenEdu.teacherBot.common.service.FileStorageService.makePublicReadable(path)))
                        .thenReturn(path))
                .map(path -> "avatars/" + fileName)
                .flatMap(relativePath -> userRepository.findById(userId)
                        .flatMap(user -> {
                            user.setAvatarUrl(relativePath);
                            return userRepository.save(user).thenReturn(relativePath);
                        }))
                .map(ApiResponse::ok);
    }

    @GetMapping("/avatar/{filename}")
    public Mono<org.springframework.core.io.Resource> getAvatar(@PathVariable String filename) {
        // Path traversal himoyasi: '..', '/' va '\' taqiqlanadi
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return Mono.error(new uz.sevenEdu.teacherBot.common.exception.NotFoundException("Fayl topilmadi"));
        }
        Path base = Paths.get("./storage/avatars").toAbsolutePath().normalize();
        Path path = base.resolve(filename).normalize();
        if (!path.startsWith(base)) {
            return Mono.error(new uz.sevenEdu.teacherBot.common.exception.NotFoundException("Fayl topilmadi"));
        }
        return Mono.just(new org.springframework.core.io.FileSystemResource(path));
    }

    /**
     * Foydalanuvchi o'z akkauntini butunlay o'chiradi (Google Play talabi).
     * Bog'liq ma'lumotlar (kurslar, progress, chat, bildirishnomalar) FK CASCADE
     * orqali o'chadi; sotuv yozuvlari anonimlanib qoladi (V35).
     * Ustoz/admin akkauntlari faqat admin panel orqali o'chiriladi.
     */
    @DeleteMapping("/me")
    public Mono<ApiResponse<Void>> deleteMyAccount(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        // Mehmon akkaunti hamma mehmonlar uchun umumiy tizim akkaunti — o'chirilsa
        // "Mehmon sifatida kirish" butunlay ishlamay qoladi.
        if (uz.sevenEdu.teacherBot.common.util.GuestUtil.GUEST_USER_ID.equals(userId)) {
            return Mono.error(new ForbiddenException("Mehmon akkauntini o'chirib bo'lmaydi"));
        }
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new NotFoundException("Foydalanuvchi topilmadi")))
                .flatMap(user -> {
                    if (user.getRole() != UserRole.STUDENT) {
                        return Mono.error(new ForbiddenException(
                                "Ustoz va admin akkauntlari administrator orqali o'chiriladi"));
                    }
                    return databaseClient.sql("UPDATE sale_records SET buyer_name = NULL WHERE user_id = :id")
                            .bind("id", userId)
                            .then()
                            .then(userRepository.deleteById(userId))
                            .then(deleteAvatarFile(user.getAvatarUrl()));
                })
                .then(Mono.just(ApiResponse.ok("Akkaunt o'chirildi", (Void) null)));
    }

    private Mono<Void> deleteAvatarFile(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("avatars/")) return Mono.empty();
        return Mono.fromRunnable(() -> {
                    try {
                        Path base = Paths.get("./storage/avatars").toAbsolutePath().normalize();
                        Path path = base.resolve(avatarUrl.substring("avatars/".length())).normalize();
                        if (path.startsWith(base)) Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Fayl o'chmasa ham akkaunt o'chirilgan hisoblanadi
                    }
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .then();
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.extractUserId(token);
    }

    private AuthResponse toResponse(BaseUser user) {
        return AuthResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
