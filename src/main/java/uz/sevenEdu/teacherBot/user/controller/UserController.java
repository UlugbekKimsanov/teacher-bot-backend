package uz.sevenEdu.teacherBot.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.user.dto.AuthResponse;
import uz.sevenEdu.teacherBot.user.dto.UpdateProfileRequest;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;
import uz.sevenEdu.teacherBot.user.security.JwtUtil;

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

    @PutMapping("/profile")
    public Mono<ApiResponse<AuthResponse>> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest request) {

        Long userId = extractUserId(authHeader);
        return userRepository.findById(userId)
                .flatMap(user -> {
                    if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
                    if (request.getLastName() != null) user.setLastName(request.getLastName());
                    if (request.getPhone() != null) user.setPhone(request.getPhone());
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
                .flatMap(path -> file.transferTo(path).thenReturn(path))
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
        Path path = Paths.get("./storage/avatars").resolve(filename);
        return Mono.just(new org.springframework.core.io.FileSystemResource(path));
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
